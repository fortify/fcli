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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionArtifactDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;

import lombok.Data;

/**
 * This descriptor defines the structure used as output for the various
 * tool commands.
 *
 * @author Ruud Senden
 */
@Reflectable // We only serialize, not de-serialize, so no need for no-args contructor
@Data
public class ToolInstallationOutputDescriptor {
    private final String name;
    private final String version;
    private final String[] aliases;
    private final String aliasesString;
    private final String stable;
    private Map<String, ToolDefinitionArtifactDescriptor> binaries;
    private final String installDir;
    private final String binDir;
    private final String installed;
    private final String __action__;
    
    public ToolInstallationOutputDescriptor(String toolName, ToolDefinitionVersionDescriptor versionDescriptor, ToolInstallationDescriptor installationDescriptor, String action) {
        this.name = toolName;
        this.version = versionDescriptor.getVersion();
        this.aliases = reverse(versionDescriptor.getAliases());
        this.aliasesString = String.join(", ", aliases);
        this.stable = versionDescriptor.isStable()?"Yes":"No";
        this.binaries = versionDescriptor.getBinaries();
        this.installDir = installationDescriptor==null ? null : installationDescriptor.getInstallDir();
        this.binDir = installationDescriptor==null ? null : installationDescriptor.getBinDir();
        this.installed = StringUtils.isBlank(this.installDir) ? "No" : "Yes";
        this.__action__ = action;
    }
    
    private static final String[] reverse(String[] array) {
        List<String> list = Arrays.asList(array);
        Collections.reverse(list);
        return list.toArray(String[]::new);
    }
}
