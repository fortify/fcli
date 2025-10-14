package com.fortify.cli.common.progress.helper;

import java.io.PrintStream;

public class ProgressWriterPrintStreamWrapper extends PrintStream {
    private final PrintStream original;
    private final IProgressWriter progressWriter;

    public ProgressWriterPrintStreamWrapper(PrintStream original, IProgressWriter progressWriter) {
        super(original);
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

    @Override
    public void close() {
        original.close();
    }
}