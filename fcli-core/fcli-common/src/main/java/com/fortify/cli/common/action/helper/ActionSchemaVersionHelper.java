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

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.common.util.SemVer;

@Reflectable public final class ActionSchemaHelper {
    private static final MessageFormat URI_FORMAT = new MessageFormat("https://fortify.github.io/fcli/schemas/action/fcli-action-schema-{0}.json");
    private static final boolean IS_FCLI_DEV_RELEASE = FcliBuildPropertiesHelper.isDevelopmentRelease();
    private static final SemVer CURRENT_SCHEMA_VERSION = new SemVer(FcliBuildPropertiesHelper.getFcliActionSchemaVersion());
    
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
        return IS_FCLI_DEV_RELEASE 
                ? true 
                : CURRENT_SCHEMA_VERSION.isCompatibleWith(version);
    }
    
    public static final String getSupportedSchemaVersionsString() {
        return IS_FCLI_DEV_RELEASE 
                ? "any (as this is an fcli development version)" 
                : CURRENT_SCHEMA_VERSION.getCompatibleVersionsString().orElse("unknown");
    }
}