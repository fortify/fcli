package com.fortify.cli.config.language.cli.cmd;

import com.fortify.cli.config.language.manager.LanguageConfigManager;

import jakarta.inject.Inject;

public abstract class AbstractLanguageCommand implements Runnable {
    @Inject public LanguageConfigManager languageConfigManager;

    @Override
    abstract public void run();
}
