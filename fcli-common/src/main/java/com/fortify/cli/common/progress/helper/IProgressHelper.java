package com.fortify.cli.common.progress.helper;

public interface IProgressHelper {
    boolean isMultiLineSupported();
    void writeProgress(String message, Object... args);
    void clearProgress();
}