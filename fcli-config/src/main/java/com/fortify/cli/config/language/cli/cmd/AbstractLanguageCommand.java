package com.fortify.cli.config.language.cli.cmd;

import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.config.language.manager.LanguageConfigManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;

@ReflectiveAccess @FixInjection
public abstract class AbstractLanguageCommand implements Runnable {
    @Inject public LanguageConfigManager languageConfigManager;

    @Override
    abstract public void run();
}
