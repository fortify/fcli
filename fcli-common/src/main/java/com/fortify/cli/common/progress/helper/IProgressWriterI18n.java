package com.fortify.cli.common.progress.helper;

public interface IProgressWriterI18n extends IProgressWriter {
    void writeI18nProgress(String keySuffix, Object... args);
    void writeI18nWarning(String keySuffix, Object... args);
}
