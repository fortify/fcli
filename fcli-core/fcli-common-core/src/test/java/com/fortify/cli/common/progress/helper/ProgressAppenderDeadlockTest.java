/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.progress.helper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.util.StdioHelper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;

class ProgressAppenderDeadlockTest {
    private static final String WORKER_MARKER = "worker-appender-marker";

    @Test
    void writerProgressLogDoesNotDeadlockWithWorkerAppend() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(
                "com.fortify.cli.common.progress.helper.ProgressAppenderDeadlockTest");
        Level previousLevel = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        var inRender = new CountDownLatch(1);
        var workerEncoded = new CountDownLatch(1);
        var encoder = new PatternLayoutEncoder() {
            @Override
            public byte[] encode(ILoggingEvent event) {
                byte[] bytes = super.encode(event);
                // Inside append(), before emit() takes the coordinator lock.
                if (event.getFormattedMessage().contains(WORKER_MARKER)) {
                    workerEncoded.countDown();
                }
                return bytes;
            }
        };
        encoder.setContext(logger.getLoggerContext());
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.setPattern("%msg%n");
        encoder.start();
        var appender = new ProgressAwareConsoleAppender();
        appender.setContext(logger.getLoggerContext());
        appender.setEncoder(encoder);
        appender.start();
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);

        PrintStream oldErr = System.err;
        var captured = new ByteArrayOutputStream();
        var capturedErr = new PrintStream(captured, true, StandardCharsets.UTF_8);
        System.setErr(capturedErr);

        var writer = new LoggingProgressWriter(logger, inRender, workerEncoded);
        var pool = Executors.newFixedThreadPool(2, task -> {
            var thread = new Thread(task, "progress-appender-deadlock");
            thread.setDaemon(true);
            return thread;
        });
        try {
            var owner = pool.submit(() -> {
                writer.writeProgress("draw");
                return null;
            });
            var worker = pool.submit(() -> {
                if (!inRender.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("render callback did not start");
                }
                logger.warn(WORKER_MARKER);
                return null;
            });
            var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            try {
                owner.get(5, TimeUnit.SECONDS);
                var remaining = deadline - System.nanoTime();
                worker.get(Math.max(0, remaining), TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                fail("progress render and console appender deadlocked", e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof AssertionError assertion) {
                    throw assertion;
                }
                throw e;
            }
            String err = captured.toString(StandardCharsets.UTF_8);
            assertTrue(err.contains(WORKER_MARKER), err);
            assertTrue(err.contains("owner-progress-draw"), err);
        } finally {
            pool.shutdownNow();
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
            logger.detachAppender(appender);
            appender.stop();
            encoder.stop();
            System.setErr(oldErr);
            capturedErr.close();
        }
    }

    @Test
    void noProgressWriterDrawsNothingWhileSimpleWriterDraws() throws Exception {
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        var progress = new ByteArrayOutputStream();
        var progressStream = new PrintStream(progress, true, StandardCharsets.UTF_8);
        System.setOut(progressStream);
        System.setErr(progressStream);
        StdioHelper.install();
        Logger logger = (Logger) LoggerFactory.getLogger(
                "com.fortify.cli.common.progress.helper.ProgressWriterType$AbstractProgressWriter");
        Level previousLevel = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        logger.setLevel(Level.OFF);
        logger.setAdditive(false);
        try {
            try (var none = ProgressWriterType.none.create()) {
                none.writeProgress("none-progress-marker-9f3c");
            }
            String afterNone = progress.toString(StandardCharsets.UTF_8);
            assertFalse(afterNone.contains("none-progress-marker-9f3c"), afterNone);

            try (var simple = ProgressWriterType.simple.create()) {
                simple.writeProgress("simple-progress-marker-9f3c");
            }
            String afterSimple = progress.toString(StandardCharsets.UTF_8);
            assertTrue(afterSimple.contains("simple-progress-marker-9f3c"), afterSimple);
            assertFalse(afterSimple.contains("none-progress-marker-9f3c"), afterSimple);
        } finally {
            logger.setAdditive(previousAdditive);
            logger.setLevel(previousLevel);
            StdioHelper.uninstall();
            System.setOut(oldOut);
            System.setErr(oldErr);
            progressStream.close();
        }
    }

    private static final class LoggingProgressWriter implements IProgressWriter {
        private final Logger logger;
        private final CountDownLatch inRender;
        private final CountDownLatch workerEncoded;

        private LoggingProgressWriter(Logger logger, CountDownLatch inRender, CountDownLatch workerEncoded) {
            this.logger = logger;
            this.inRender = inRender;
            this.workerEncoded = workerEncoded;
        }

        @Override
        public boolean isMultiLineSupported() { return false; }

        @Override
        public void writeProgress(String message, Object... args) {
            ProgressRenderCoordinator.render(this, () -> {
                inRender.countDown();
                try {
                    if (!workerEncoded.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("worker did not encode inside append");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("interrupted while waiting for worker append", e);
                }
                logger.debug("owner-progress-draw");
            });
        }

        @Override
        public void clearProgress() {
            ProgressRenderCoordinator.clear(this, () -> { });
        }

        @Override public void writeInfo(String message, Object... args) {}
        @Override public void writeInfoWithException(String message, Throwable cause, Object... args) {}
        @Override public void writeWarning(String message, Object... args) {}
        @Override public void writeWarningWithException(String message, Throwable cause, Object... args) {}
        @Override public void close() { clearProgress(); }
        @Override public String type() { return "logging"; }
    }
}
