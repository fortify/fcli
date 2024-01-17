/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.tool._common.helper;

import java.util.Arrays;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.StringUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class represents the contents of a tool definition YAML file, usually
 * deserialized from [tool-name].yaml loaded from tool-definitions.yaml.zip. 
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@Reflectable @NoArgsConstructor
@Data
public class ToolDefinitionRootDescriptor {
    private String schema_version;
    private ToolDefinitionVersionDescriptor[] versions;
    
    public final ToolDefinitionVersionDescriptor[] getVersions() {
        return getVersionsStream().toArray(ToolDefinitionVersionDescriptor[]::new);
    }
    
    public final Stream<ToolDefinitionVersionDescriptor> getVersionsStream() {
        return Stream.of(versions);
    }
    
    public final ToolDefinitionVersionDescriptor getVersion(String versionOrAlias) {
        return getVersionsStream()
                .filter(v-> (v.getVersion().equals(versionOrAlias) || Arrays.stream(v.getAliases()).anyMatch(versionOrAlias::equals)) )
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Version or alias "+versionOrAlias+" not found"));
    }
    
    public final ToolDefinitionVersionDescriptor getVersionOrDefault(String versionOrAlias) {
        if ( StringUtils.isBlank(versionOrAlias) || "default".equals(versionOrAlias)) {
            versionOrAlias = "latest";
        }
        return getVersion(versionOrAlias);
    }
    
}