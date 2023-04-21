package com.fortify.cli.common.progress.helper;

public interface IProgressHelper extends AutoCloseable {
    boolean isMultiLineSupported();
    void writeProgress(String message, Object... args);
    void writeWarning(String message, Object... args);
    void clearProgress();
    void close();
}