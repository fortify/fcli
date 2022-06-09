package com.fortify.cli.common.locale;

import jakarta.inject.Inject;

public abstract class AbstractLanguageCommand implements Runnable {
    @Inject public LanguageHelper languageHelper;

    @Override
    abstract public void run();
}
