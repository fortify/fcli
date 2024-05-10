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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.SneakyThrows;

// We'll probably need to think about this a bit more, but for now, the 
// SupportedSchemaVersion enum lists schema versions supported by this fcli version.
// The JsonProperty annotations on the enum entries define the actual version numbers,
// enum entry names can be used to reference specific schema versions in code. For now,
// only the 'current' enum entry name is used explicitly to generate the proper schema
// file name from the GenerateActionSchema class (in fcli-other/fcli-doc). Other enum
// entries may have arbitrary names, but it is recommended to use a name that matches
// the version number, for example V1_1.
//
// Simple changes that don't affect schema/yaml structure, like updating property
// descriptions, may be performed without updating the schema version; we'll just 
// publish such changes under the current schema version. 
//
// For any structural changes, we'll want to update the SupportedSchemaVersion enum 
// accordingly:
// - If we introduce a non-breaking change (like adding a new non-required property), 
//   the 'current' enum entry should be copied as a new entry named after the actual 
//   version, i.e.: '@JsonProperty("1.0") V1_0'. The JsonProperty value for 'current'
//   should then be updated to the next minor version. This will allow the current
//   fcli version to support (for example) both '1.0' and '1.1' schema versions, and
//   will result in he GenerateActionSchema class to use the 'current' '1.1' version
//   as schema file name. If a user attempts to use an action with schema version '1.1'
//   on older fcli versions, this will generate an exception message stating that '1.1'
//   is not a supported value for schemaVersion (we should probably improve the rather 
//   technical exception stack traces & messages).
// - If we introduce a breaking change (like adding a new required property that's
//   not inside a new optional property, or when removing properties), all existing
//   explicit schema versions should be removed, and the JsonProperty value of the
//   'current' entry should be updated to the next major version, i.e., '2.0'. This
//   means that current fcli version can no longer process actions with schemaVersion
//   1.x, and obviously older fcli versions won't be able to process actions with 
//   schemaVersion 2.x.
// Some notes:
// - For breaking changes, we could potentially provide backward compatibility by
//   maintaining two copies of the Action class tree (and corresponding supporting
//   classes like ActionRunner); one set of classes for the older major version, 
//   one set of classes for the newer major version. We can then use the schemaVersion 
//   property to select the appropriate set of classes. Not sure whether this is
//   worth the increased maintenance effort though.
// - If there's any need to rename properties, potentially we can avoid a major version
//   change by keeping setters for the legacy property name, potentially outputting a 
//   deprecation warning if any of these legacy setters are being invoked.
// - If we'd want to publish our schema to schemastore.org for example, ideally we'd need
//   to have an overall schema that supports multiple versions, giving users code completion
//   and validation based on schemaVersion. Not sure whether this is possible, but one idea
//   is to have an overall schema that uses oneOf or if/then/else to reference each of the 
//   versioned schema's based on schemaVersion provided in the action yaml file. So, if the 
//   user types 'schemaVersion: 1.0', the editor would automatically select the 1.0 version
//   of the schema.
@Reflectable public enum SupportedSchemaVersion {
    @JsonProperty("1.0") current,
    // Uncomment the following line if we update 'current' to '1.1' for example:
    //   JsonProperty("1.0") V1_0,
    // On every schema version increase, we'd add a similar enum entry for all
    // supported 1.x versions, until we introduce a breaking change and change 
    // 'current' version to '2.0', in which case we'd remove all 1.x enum entries.
    ;
    
    @Override @SneakyThrows
    public String toString() {
        return SupportedSchemaVersion.class
                .getField(name())
                .getAnnotation(JsonProperty.class)
                .value();
    }
}