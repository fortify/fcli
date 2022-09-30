package com.fortify.cli.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import picocli.CommandLine.IVersionProvider;

public class FortifyCLIVersionProvider implements IVersionProvider {
    private static final Properties buildProperties = loadProperties();

    @Override
    public String[] getVersion() throws Exception {
        return new String[] {String.format("%s version %s, built on %s" 
                , buildProperties.getProperty("projectName", "fcli")
                , buildProperties.getProperty("projectVersion", "unknown")
                , buildProperties.getProperty("buildDate", "unknown"))};
    }

    private static final Properties loadProperties() {
        final Properties p = new Properties();
        try (final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("com/fortify/cli/app/fcli-build.properties")) {
            if ( stream!=null ) { p.load(stream); }
        } catch ( IOException ioe ) {
            System.err.println("Error reading fcli-build.properties from classpath");
        }
        return p;
    }

}
