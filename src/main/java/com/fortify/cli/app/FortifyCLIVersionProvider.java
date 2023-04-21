package com.fortify.cli.app;

import com.fortify.cli.common.util.FcliBuildPropertiesHelper;

import picocli.CommandLine.IVersionProvider;

public class FortifyCLIVersionProvider implements IVersionProvider {
    @Override
    public final String[] getVersion() throws Exception {
        return new String[] {FcliBuildPropertiesHelper.getFcliBuildInfo()};
    }
}
