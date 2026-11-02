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

/**
 * Clears visible progress and writes original stdout/stderr under the {@link ProgressRenderCoordinator} lock.
 * A blocked original stream also blocks progress rendering and console log output.
 * Captured and suppressed output bypasses that lock and does not clear terminal progress.
 */
public class ProgressWriterPrintStreamWrapper extends PrintStream {
    private final PrintStream original;

    public ProgressWriterPrintStreamWrapper(PrintStream original) {
        super(original);
        this.original = original;
    }

    PrintStream original() {
        return original;
    }

    @Override
    public void close() {
        original.flush();
    }

    @Override
    public void write(int b) {
        ProgressRenderCoordinator.emit(original, () -> original.write(b));
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        ProgressRenderCoordinator.emit(original, () -> original.write(buf, off, len));
    }

    @Override
    public void print(String s) {
        ProgressRenderCoordinator.emit(original, () -> original.print(s));
    }

    @Override
    public void println() {
        ProgressRenderCoordinator.emit(original, original::println);
    }

    @Override
    public void println(String s) {
        ProgressRenderCoordinator.emit(original, () -> original.println(s));
    }

    @Override
    public void println(Object o) {
        println(String.valueOf(o));
    }

    @Override
    public void flush() {
        original.flush();
    }
}