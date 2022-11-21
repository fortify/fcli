package com.fortify.cli.app;

import java.util.Properties;

import com.fortify.cli.common.util.FcliBuildPropertiesHelper;

import picocli.CommandLine.IVersionProvider;

public class FortifyCLIVersionProvider implements IVersionProvider {
    @Override
    public final String[] getVersion() throws Exception {
        Properties buildProperties = FcliBuildPropertiesHelper.getBuildProperties();
        return new String[] {String.format("%s version %s, built on %s" 
                , buildProperties.getProperty("projectName", "fcli")
                , buildProperties.getProperty("projectVersion", "unknown")
                , buildProperties.getProperty("buildDate", "unknown"))};
    }
}
