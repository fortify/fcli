/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliBugException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FcliBuildProperties {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final FcliBuildProperties INSTANCE = new FcliBuildProperties();
    @JsonIgnore @Getter private final Properties buildProperties = loadProperties();
    
    public final boolean isDevelopmentRelease() {
        var version = getFcliVersion();
        return version.startsWith("0.") || version.equals("unknown");
    }
    
    public final String getFcliProjectName() {
        return buildProperties.getProperty("projectName", "fcli");
    }
    
    public final String getFcliVersion() {
        return buildProperties.getProperty("projectVersion", "unknown");
    }
    
    public final String getFcliBuildDateString() {
        return buildProperties.getProperty("buildDate", "unknown");
    }
    
    public final Date getFcliBuildDate() {
        var dateString = getFcliBuildDateString();
        if ( !StringUtils.isBlank(dateString) && !"unknown".equals(dateString) ) {
            try {
                return DATE_FORMAT.parse(dateString);
            } catch (ParseException ignore) {}
        }
        return null;
    }
    
    public final String getFcliActionSchemaVersion() {
        return buildProperties.getProperty("actionSchemaVersion", "unknown");
    }
    
    public final String getRepoBranchOrTag() {
        // TODO Determine correct dev branch, to avoid incorrect links if we ever move to dev/v4.x
        return isDevelopmentRelease() ? "dev/v3.x" : String.format("v%s", getFcliVersion());
    }
    
    public final String getSourceCodeBaseUrl() {
        return "https://github.com/fortify/fcli/blob/"+getRepoBranchOrTag();
    }
    
    public final String getFcliDocBaseUrl() {
        return isDevelopmentRelease()
                ? "https://fortify.github.io/fcli/latest_dev"
                : String.format("https://fortify.github.io/fcli/v%s", getFcliVersion());
    }
    
    public final String getFcliActionSchemaUrl() {
        return String.format("https://fortify.github.io/fcli/schemas/action/fcli-action-schema-%s.json", getFcliActionSchemaVersion());
    }
    
    public final String getFcliBuildInfo() {
        return String.format("%s version %s, built on %s" 
                , getFcliProjectName()
                , getFcliVersion()
                , getFcliBuildDateString());
    }

    private static final Properties loadProperties() {
        final Properties p = new Properties();
        try (final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("com/fortify/cli/common/fcli-build.properties")) {
            if ( stream!=null ) { p.load(stream); }
        } catch ( IOException ioe ) {
            throw new FcliBugException("Error reading fcli-build.properties from classpath", ioe);
        }
        return p;
    }
}
