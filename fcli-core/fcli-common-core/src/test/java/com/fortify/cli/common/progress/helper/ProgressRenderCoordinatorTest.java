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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

class ProgressRenderCoordinatorTest {
    @Test
    void nestedWritersClearOnlyTheVisibleLineBeforeAWarning() {
        List<String> events = new ArrayList<>();
        var outer = new RecordingWriter("outer", events);
        var inner = new RecordingWriter("inner", events);

        outer.writeProgress("first");
        inner.writeProgress("second");
        ProgressRenderCoordinator.emit(() -> events.add("warning"));
        outer.writeProgress("third");
        outer.clearProgress();

        assertEquals(List.of("outer:first", "outer:clear", "inner:second", "inner:clear",
                "warning", "outer:third", "outer:clear"), events);
    }

    @Test
    void concurrentProgressAndWarningsNeverEmitOverAVisibleLine() throws Exception {
        var writer = new RecordingWriter("writer", null);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var progress = pool.submit(() -> {
                start.await();
                for (int i = 0; i < 1_000; i++) writer.writeProgress("progress");
                return null;
            });
            var warnings = pool.submit(() -> {
                start.await();
                for (int i = 0; i < 1_000; i++) {
                    ProgressRenderCoordinator.emit(() -> assertFalse(writer.visible));
                }
                return null;
            });
            start.countDown();
            progress.get(10, TimeUnit.SECONDS);
            warnings.get(10, TimeUnit.SECONDS);
        } finally {
            writer.clearProgress();
            pool.shutdownNow();
        }
    }

    @Test
    void realConsoleAppenderDoesNotDeadlockWithOwnerAndWorkerWarnings() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(
                "com.fortify.cli.common.progress.helper.ProgressWriterType$AbstractProgressWriter");
        Level previousLevel = logger.getLevel();
        boolean previousAdditivity = logger.isAdditive();
        var encoder = new PatternLayoutEncoder();
        encoder.setContext(logger.getLoggerContext());
        encoder.setPattern("%msg%n%ex{0}");
        encoder.start();
        var appender = new ProgressAwareConsoleAppender();
        appender.setContext(logger.getLoggerContext());
        appender.setEncoder(encoder);
        appender.start();
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.WARN);

        var writerRef = new AtomicReference<IProgressWriter>();
        var writerReady = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2, task -> {
            var thread = new Thread(task, "progress-render-regression");
            thread.setDaemon(true);
            return thread;
        });
        try {
            var owner = pool.submit(() -> {
                try (var writer = ProgressWriterType.single_line.create()) {
                    writerRef.set(writer);
                    writerReady.countDown();
                    for (int i = 0; i < 500; i++) {
                        writer.writeProgress("progress " + i);
                        logger.warn("owner warning {}", i);
                    }
                }
                return null;
            });
            var worker = pool.submit(() -> {
                if (!writerReady.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Owner did not create its progress writer");
                }
                for (int i = 0; i < 500; i++) {
                    writerRef.get().writeWarning("worker warning " + i);
                }
                return null;
            });
            owner.get(15, TimeUnit.SECONDS);
            worker.get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditivity);
            logger.detachAppender(appender);
            appender.stop();
            encoder.stop();
        }
    }

    private static final class RecordingWriter implements IProgressWriter {
        private final String name;
        private final List<String> events;
        private boolean visible;

        private RecordingWriter(String name, List<String> events) {
            this.name = name;
            this.events = events;
        }

        @Override public boolean isMultiLineSupported() { return false; }
        @Override public void writeProgress(String message, Object... args) {
            ProgressRenderCoordinator.render(this, () -> {
                visible = true;
                if (events != null) events.add(name + ":" + message);
            });
        }
        @Override public void clearProgress() {
            ProgressRenderCoordinator.clear(this, () -> {
                if (visible && events != null) events.add(name + ":clear");
                visible = false;
            });
        }
        @Override public void writeInfo(String message, Object... args) {}
        @Override public void writeInfoWithException(String message, Throwable cause, Object... args) {}
        @Override public void writeWarning(String message, Object... args) {}
        @Override public void writeWarningWithException(String message, Throwable cause, Object... args) {}
        @Override public void close() { clearProgress(); }
        @Override public String type() { return "recording"; }
    }
}
