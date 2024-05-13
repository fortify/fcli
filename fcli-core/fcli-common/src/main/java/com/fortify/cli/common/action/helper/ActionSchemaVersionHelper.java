/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.common.action.helper;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;

@Reflectable public final class ActionSchemaVersionHelper {
    private static final MessageFormat URI_FORMAT = new MessageFormat("https://fortify.github.io/fcli/schemas/action/fcli-action-schema-{0}.json");
    
    /** Get the schema URI for the current enum entry by formatting schema version as URI */
    public static final String toURI(String version) {
        return URI_FORMAT.format(new Object[] {version});
    }

    /** Check whether given schema/version is supported */
    public static final boolean isSupportedSchemaURI(String schemaURI) {
        return isSupportedSchemaVersion(getSchemaVersion(schemaURI));
    }
    
    public static final String getSchemaVersion(String schemaURI) {
        try {
            return (String)URI_FORMAT.parse(schemaURI)[0];
        } catch (ParseException e) {
            return "unknown";
        }
    }

    /** Check whether given schema version is supported */
    public static final boolean isSupportedSchemaVersion(String version) {
        return getSupportedSchemaVersions().contains(version);
    }
    
    public static final List<String> getSupportedSchemaVersions() {
        var fcliVersion = FcliBuildPropertiesHelper.getFcliVersion();
        return Arrays.asList(fcliVersion.startsWith("0.")?"dev":fcliVersion);
    }
}