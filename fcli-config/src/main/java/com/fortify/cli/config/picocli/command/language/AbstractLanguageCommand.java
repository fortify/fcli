package com.fortify.cli.config.picocli.command.language;

import com.fortify.cli.common.config.LanguageConfig;

import jakarta.inject.Inject;

public abstract class AbstractLanguageCommand implements Runnable {
    @Inject public LanguageConfig languageConfig;

    @Override
    abstract public void run();
}
