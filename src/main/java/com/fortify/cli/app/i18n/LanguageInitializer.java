package com.fortify.cli.app.i18n;

import java.util.Locale;

import com.fortify.cli.common.cli.util.FortifyCLIInitializerRunner.FortifyCLIInitializerCommand;
import com.fortify.cli.common.cli.util.IFortifyCLIInitializer;
import com.fortify.cli.common.i18n.helper.LanguageHelper;

import jakarta.inject.Singleton;

@Singleton
public class LanguageInitializer implements IFortifyCLIInitializer {
    @Override
    public void initializeFortifyCLI(FortifyCLIInitializerCommand cmd) {
        Locale.setDefault(LanguageHelper.getConfiguredLanguageDescriptor().getLocale());
    }
}