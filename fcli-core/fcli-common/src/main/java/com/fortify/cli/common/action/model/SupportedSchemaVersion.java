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
package com.fortify.cli.common.action.model;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.SneakyThrows;

/**
 * <p>This enum lists schema versions supported by this fcli release; the 
 * JsonProperty annotations on the enum entries define the actual version numbers.</p>
 * 
 * <p>Simple changes that don't affect schema/yaml structure (like updating property
 * descriptions) or property interpretation (like no change from plain text to template 
 * expression), may be performed without updating the schema version; we'll just 
 * publish such changes under the current schema version.</p> 
 * 
 * <p>For any structural changes, we'll want to update the SupportedSchemaVersion enum 
 * accordingly:</p>
 * <ul>
 *  <li>If we introduce a non-breaking change (like adding a new non-required property), 
 *   the 'current' enum entry should be copied as a new entry named after the actual 
 *   version, i.e.: '@JsonProperty("1.0") V1_0'. The JsonProperty value for 'current'
 *   should then be updated to the next minor version. This will allow the current
 *   fcli version to support (for example) both '1.0' and '1.1' schema versions, and
 *   will result in he GenerateActionSchema class to use the 'current' '1.1' version
 *   as schema file name. If a user attempts to use an action with schema version '1.1'
 *   on older fcli versions, this will generate an exception message stating that '1.1'
 *   is not a supported value for schemaVersion (we should probably improve the rather 
 *   technical exception stack traces & messages).</li>
 *  <li>If we introduce a breaking change (like adding a new required property that's
 *   not inside a new optional property, or when removing properties), all existing
 *   explicit schema versions should be removed, and the JsonProperty value of the
 *   'current' entry should be updated to the next major version, i.e., '2.0'. This
 *   means that current fcli version can no longer process actions with schemaVersion
 *   1.x, and obviously older fcli versions won't be able to process actions with 
 *   schemaVersion 2.x.</li>
 * </ul>
 * 
 * <p>Some notes:</p>
 * <ul>
 *  <li>For breaking changes, we could potentially provide backward compatibility by
 *   maintaining two copies of the Action class tree (and corresponding supporting
 *   classes like ActionRunner); one set of classes for the older major version, 
 *   one set of classes for the newer major version. We can then use the schemaVersion 
 *   property to select the appropriate set of classes. Not sure whether this is
 *   worth the increased maintenance effort though.</li>
 *  <li>If there's any need to rename properties, potentially we can avoid a major version
 *   change by keeping setters for the legacy property name, potentially outputting a 
 *   deprecation warning if any of these legacy setters are being invoked.</li>
 * </ul>
 */

@Reflectable public enum SupportedSchemaVersion {
    @JsonProperty("1.0") current,
    // Uncomment the following line if we update 'current' to '1.1' for example:
    //   @JsonProperty("1.0") V1_0,
    // On every schema version increase, we'd add a similar enum entry for all
    // supported 1.x versions, until we introduce a breaking change and change 
    // 'current' version to '2.0', in which case we'd remove all 1.x enum entries.
    ;
    
    private static final MessageFormat URI_FORMAT = new MessageFormat("https://fortify.github.io/fcli/schemas/action/fcli-action-schema-{0}.json");
    
    /** Get the version number for the current enum entry from the JsonProperty annotation */
    @SneakyThrows
    public String toVersion() {
        return SupportedSchemaVersion.class
                .getField(name())
                .getAnnotation(JsonProperty.class)
                .value();
    }
    
    /** Get the schema URI for the current enum entry by formatting schema version as URI */
    public String toURI() {
        return URI_FORMAT.format(new Object[] {this.toVersion()});
    }

    /** Check whether given schema/version is supported */
    public static final boolean isSupportedSchemaURI(String schema) {
        try {
            return isSupportedSchemaVersion((String)URI_FORMAT.parse(schema)[0]);
        } catch (ParseException e) {
            return false;
        }
    }

    /** Check whether given schema version is supported */
    public static final boolean isSupportedSchemaVersion(String version) {
        return Stream.of(SupportedSchemaVersion.values())
                .map(SupportedSchemaVersion::toVersion)
                .anyMatch(version::equals);
    }
}