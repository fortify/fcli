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

import java.io.PrintStream;

import com.fortify.cli.common.cli.util.StdioHelper;

/** Coordinates terminal progress with complete console log events. */
public final class ProgressRenderCoordinator {
    private static final Object LOCK = new Object();
    private static IProgressWriter visibleWriter;

    private ProgressRenderCoordinator() {}

    static void render(IProgressWriter writer, Runnable render) {
        synchronized (LOCK) {
            if (visibleWriter != null && visibleWriter != writer) {
                visibleWriter.clearProgress();
            }
            render.run();
            visibleWriter = writer;
        }
    }

    static void clear(IProgressWriter writer, Runnable clear) {
        synchronized (LOCK) {
            clear.run();
            if (visibleWriter == writer) visibleWriter = null;
        }
    }

    /**
     * Clear any visible progress and run one complete write. The lock is held while {@code emit} runs,
     * so it must not block for long.
     */
    static void emit(Runnable emit) {
        synchronized (LOCK) {
            if (visibleWriter != null) visibleWriter.clearProgress();
            emit.run();
        }
    }

    /**
     * Coordinate original stdout and stderr together, since both can share a terminal.
     * Captured or suppressed output must not clear unrelated terminal progress.
     */
    static void emit(PrintStream destination, Runnable emit) {
        while (destination instanceof ProgressWriterPrintStreamWrapper wrapper) {
            destination = wrapper.original();
        }
        if (StdioHelper.isOriginalStream(destination)) {
            emit(emit);
        } else {
            emit.run();
        }
    }
}
