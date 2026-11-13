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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.util.StdioHelper;
import com.fortify.cli.common.util.ConsoleHelper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * The lazy terminal width initializer logs a warning. A single-line writer must resolve the
 * width before taking the render lock; otherwise it holds the lock while waiting for the
 * initializer, and the initializer waits for the lock to emit its warning.
 */
class ProgressTerminalWidthLockOrderTest {
    private static final String WIDTH_PROPERTY = "fcli.terminal.width";

    @Test
    void singleLineProgressDoesNotDeadlockWithTerminalWidthInitialization() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ConsoleHelper.class);
        Level previousLevel = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        String previousProperty = System.getProperty(WIDTH_PROPERTY);
        var initializerLogging = new CountDownLatch(1);
        var progressThread = new AtomicReference<Thread>();
        var holdsLockWhileBlocked = new AtomicBoolean();
        var encoder = new PatternLayoutEncoder() {
            @Override
            public byte[] encode(ILoggingEvent event) {
                if (event.getLevel() == Level.WARN) {
                    initializerLogging.countDown();
                    if (progressThreadBlockedHoldingRenderLock(progressThread)) {
                        // Fail the append instead of deadlocking, which would also hang later tests.
                        holdsLockWhileBlocked.set(true);
                        throw new IllegalStateException("progress thread holds the render lock while blocked");
                    }
                }
                return super.encode(event);
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
        logger.setLevel(Level.WARN);

        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        var captured = new ByteArrayOutputStream();
        var capturedStream = new PrintStream(captured, true, StandardCharsets.UTF_8);
        System.setOut(capturedStream);
        System.setErr(capturedStream);
        StdioHelper.install();
        System.setProperty(WIDTH_PROPERTY, "not-a-number");
        resetTerminalWidth();
        var pool = Executors.newFixedThreadPool(2, task -> {
            var thread = new Thread(task, "terminal-width-lock-order");
            thread.setDaemon(true);
            return thread;
        });
        var writer = ProgressWriterType.single_line.create();
        try {
            var initializer = pool.submit(() -> ConsoleHelper.getTerminalWidth());
            var progress = pool.submit(() -> {
                if (!initializerLogging.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("terminal width initializer did not log");
                }
                progressThread.set(Thread.currentThread());
                writer.writeProgress("progress-marker");
                return null;
            });
            try {
                initializer.get(10, TimeUnit.SECONDS);
                progress.get(10, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                fail("single-line progress and terminal width initialization deadlocked", e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof AssertionError assertion) { throw assertion; }
                throw e;
            }
            assertFalse(holdsLockWhileBlocked.get(), "progress thread waited for the terminal width while holding the render lock");
            writer.close();
            String output = captured.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Invalid value for system property"), output);
        } finally {
            pool.shutdownNow();
            if (previousProperty == null) {
                System.clearProperty(WIDTH_PROPERTY);
            } else {
                System.setProperty(WIDTH_PROPERTY, previousProperty);
            }
            resetTerminalWidth();
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
            logger.detachAppender(appender);
            appender.stop();
            encoder.stop();
            StdioHelper.uninstall();
            System.setOut(oldOut);
            System.setErr(oldErr);
            capturedStream.close();
        }
    }

    // True if the progress thread is blocked while it holds the render lock; polls up to 5 seconds for it to block.
    private static boolean progressThreadBlockedHoldingRenderLock(AtomicReference<Thread> progressThread) {
        try {
            var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            Thread thread;
            while ((thread = progressThread.get()) == null || thread.getState() != Thread.State.BLOCKED) {
                if (System.nanoTime() > deadline) { return false; }
                Thread.sleep(10);
            }
            Field field = ProgressRenderCoordinator.class.getDeclaredField("LOCK");
            field.setAccessible(true);
            int lockHash = System.identityHashCode(field.get(null));
            ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(new long[] {thread.getId()}, true, false)[0];
            return info != null && Arrays.stream(info.getLockedMonitors())
                    .anyMatch(monitor -> monitor.getIdentityHashCode() == lockHash);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // Lombok's lazy getter caches the value in this AtomicReference; null means not initialized.
    @SuppressWarnings("unchecked")
    private static void resetTerminalWidth() throws Exception {
        Field field = ConsoleHelper.class.getDeclaredField("terminalWidth");
        field.setAccessible(true);
        ((AtomicReference<Object>) field.get(null)).set(null);
    }
}
