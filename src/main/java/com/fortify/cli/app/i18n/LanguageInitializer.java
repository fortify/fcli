package com.fortify.cli.app.i18n;

import java.util.Locale;

import com.fortify.cli.app.IFortifyCLIInitializer;
import com.fortify.cli.config.language.manager.LanguageConfigManager;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class LanguageInitializer implements IFortifyCLIInitializer {
    private final LanguageConfigManager config;

    @Inject
    public LanguageInitializer(LanguageConfigManager config) {
        this.config = config;
    }

    @Override
    public void initializeFortifyCLI(String[] args) {
        Locale.setDefault(config.getLocale());
    }
}