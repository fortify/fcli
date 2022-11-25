package com.fortify.cli.common.cli.util;

import com.fortify.cli.common.cli.util.FortifyCLIInitializerRunner.FortifyCLIInitializerCommand;

import jakarta.inject.Singleton;

@Singleton
public class EnvPrefixInitializer implements IFortifyCLIInitializer {
    @Override
    public void initializeFortifyCLI(FortifyCLIInitializerCommand cmd) {
        String envPrefix = cmd.getGenericOptions().getEnvPrefix();
        System.setProperty("fcli.env.default.prefix", envPrefix);
        FortifyCLIDefaultValueProvider.setEnvPrefix(envPrefix);
    }
}
