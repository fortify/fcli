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

import java.util.Map;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.StringUtils;

import lombok.Data;

/**
 * This descriptor defines the structure used as output for the various
 * tool commands.
 *
 * @author Ruud Senden
 */
@Reflectable // We only serialize, not de-serialize, so no need for no-args contructor
@Data
public class ToolOutputDescriptor {
    private final String name;
    private final String version;
    private final String aliasFor;
    private final boolean stable;
    private Map<String, ToolDefinitionArtifactDescriptor> artifacts;
    private final String installDir;
    private final String binDir;
    private final String installed;
    
    public ToolOutputDescriptor(String toolName, String version, ToolDefinitionVersionDescriptor versionDescriptor, ToolInstallationDescriptor installationDescriptor) {
        this.name = toolName;
        this.version = version;
        this.aliasFor = versionDescriptor.getVersion();
        this.stable = versionDescriptor.isStable();
        this.artifacts = versionDescriptor.getArtifacts();
        this.installDir = installationDescriptor==null ? null : installationDescriptor.getInstallDir();
        this.binDir = installationDescriptor==null ? null : installationDescriptor.getBinDir();
        this.installed = StringUtils.isBlank(this.installDir) ? "No" : "Yes";
    }
    
    public ToolOutputDescriptor(String toolName, ToolDefinitionVersionDescriptor versionDescriptor, ToolInstallationDescriptor installationDescriptor) {
        this(toolName, versionDescriptor.getVersion(), versionDescriptor, installationDescriptor);
    }
}
