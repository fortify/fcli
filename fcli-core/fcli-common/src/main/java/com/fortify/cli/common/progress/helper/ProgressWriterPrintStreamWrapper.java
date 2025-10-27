/*
 * Copyright 2021-2025 Open Text.
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

import com.fortify.cli.common.util.NonClosingPrintStream;

public class ProgressWriterPrintStreamWrapper extends NonClosingPrintStream {
    private final PrintStream original;
    private final IProgressWriter progressWriter;

    public ProgressWriterPrintStreamWrapper(String name, PrintStream original, IProgressWriter progressWriter) {
        super(name, original);
        this.original = original;
        this.progressWriter = progressWriter;
    }

    private void clearProgress() {
        progressWriter.clearProgress();
    }

    @Override
    public void write(int b) {
        clearProgress();
        original.write(b);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        clearProgress();
        original.write(buf, off, len);
    }

    @Override
    public void print(String s) {
        clearProgress();
        original.print(s);
    }

    @Override
    public void println(String s) {
        clearProgress();
        original.println(s);
    }

    @Override
    public void flush() {
        original.flush();
    }
}