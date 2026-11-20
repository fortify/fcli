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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

/** Ordinary output must not be written after another thread has redrawn progress. */
class ProgressWriterPrintStreamWrapperRaceTest {
    @Test
    void outputIsWrittenWithoutVisibleProgress() throws Exception {
        var inWrite = new CountDownLatch(1);
        var progressRendered = new CountDownLatch(1);
        var visible = new AtomicBoolean();
        var visibleWhenWritten = new AtomicBoolean();
        var sink = new ByteArrayOutputStream();
        var original = new PrintStream(sink, true, StandardCharsets.UTF_8) {
            @Override
            public void print(String s) {
                inWrite.countDown();
                try {
                    // Gives another thread the chance to redraw progress right before the write.
                    progressRendered.await(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                visibleWhenWritten.set(visible.get());
                super.print(s);
            }
        };
        var writer = new VisibleFlagProgressWriter(visible);
        var wrapper = new ProgressWriterPrintStreamWrapper(original);
        var oldOut = System.out;
        System.setOut(original);
        var pool = Executors.newFixedThreadPool(2, task -> {
            var thread = new Thread(task, "progress-wrapper-race");
            thread.setDaemon(true);
            return thread;
        });
        try {
            writer.writeProgress("progress");
            var output = pool.submit(() -> wrapper.print("data"));
            var redraw = pool.submit(() -> {
                if (!inWrite.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("output thread did not start writing");
                }
                writer.writeProgress("progress");
                progressRendered.countDown();
                return null;
            });
            output.get(10, TimeUnit.SECONDS);
            redraw.get(10, TimeUnit.SECONDS);
            assertFalse(visibleWhenWritten.get(), "output was written while progress was visible");
            assertEquals("data", sink.toString(StandardCharsets.UTF_8));
        } finally {
            pool.shutdownNow();
            writer.close();
            System.setOut(oldOut);
        }
    }

    @Test
    void printlnObjectIsWrittenWithoutVisibleProgress() throws Exception {
        var visible = new AtomicBoolean();
        var visibleWhenWritten = new AtomicBoolean();
        var sink = new ByteArrayOutputStream();
        var original = new PrintStream(sink, true, StandardCharsets.UTF_8) {
            @Override
            public void println(String s) {
                visibleWhenWritten.set(visible.get());
                super.println(s);
            }
        };
        var writer = new VisibleFlagProgressWriter(visible);
        var wrapper = new ProgressWriterPrintStreamWrapper(original);
        var oldOut = System.out;
        System.setOut(original);
        try {
            writer.writeProgress("progress");
            wrapper.println((Object) "data");
            assertFalse(visibleWhenWritten.get(), "output was written while progress was visible");
            assertEquals("data" + System.lineSeparator(), sink.toString(StandardCharsets.UTF_8));
        } finally {
            writer.close();
            System.setOut(oldOut);
        }
    }

    private static final class VisibleFlagProgressWriter implements IProgressWriter {
        private final AtomicBoolean visible;

        private VisibleFlagProgressWriter(AtomicBoolean visible) {
            this.visible = visible;
        }

        @Override
        public boolean isMultiLineSupported() { return false; }

        @Override
        public void writeProgress(String message, Object... args) {
            ProgressRenderCoordinator.render(this, () -> visible.set(true));
        }

        @Override
        public void clearProgress() {
            ProgressRenderCoordinator.clear(this, () -> visible.set(false));
        }

        @Override public void writeInfo(String message, Object... args) {}
        @Override public void writeInfoWithException(String message, Throwable cause, Object... args) {}
        @Override public void writeWarning(String message, Object... args) {}
        @Override public void writeWarningWithException(String message, Throwable cause, Object... args) {}
        @Override public void close() { clearProgress(); }
        @Override public String type() { return "visible-flag"; }
    }
}
