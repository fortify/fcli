/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FcliBuildPropertiesHelper {
    private static final Properties buildProperties = loadProperties();
    
    public static final Properties getBuildProperties() {
        return buildProperties;
    }
    
    public static final String getFcliProjectName() {
        return buildProperties.getProperty("projectName", "fcli");
    }
    
    public static final String getFcliVersion() {
        return buildProperties.getProperty("projectVersion", "unknown");
    }
    
    public static final String getFcliBuildDate() {
        return buildProperties.getProperty("buildDate", "unknown");
    }
    
    public static final String getFcliBuildInfo() {
        return String.format("%s version %s, built on %s" 
                , FcliBuildPropertiesHelper.getFcliProjectName()
                , FcliBuildPropertiesHelper.getFcliVersion()
                , FcliBuildPropertiesHelper.getFcliBuildDate());
    }

    private static final Properties loadProperties() {
        final Properties p = new Properties();
        try (final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("com/fortify/cli/app/fcli-build.properties")) {
            if ( stream!=null ) { p.load(stream); }
        } catch ( IOException ioe ) {
            throw new RuntimeException("Error reading fcli-build.properties from classpath", ioe);
        }
        return p;
    }

}
