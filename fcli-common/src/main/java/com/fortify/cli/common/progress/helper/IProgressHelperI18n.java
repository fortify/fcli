package com.fortify.cli.common.progress.helper;

public interface IProgressHelperI18n extends IProgressHelper {
    void writeI18nProgress(String keySuffix, Object... args);
    void writeI18nWarning(String keySuffix, Object... args);
}
