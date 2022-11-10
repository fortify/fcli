package com.fortify.cli.fod.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FoDUtils {
    public static final Properties loadProperties() {
        final Properties p = new Properties();
        try (final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("com/fortify/cli/app/fcli-build.properties")) {
            if (stream != null) {
                p.load(stream);
            }
        } catch (IOException ioe) {
            System.err.println("Error reading fcli-build.properties from classpath");
        }
        return p;
    }
}
