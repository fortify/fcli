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
package com.fortify.cli.tool._common.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.formkiq.graalvm.annotations.Reflectable;
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
    private Map<String, String> extraProperties;
    private final String installDir;
    private final String binDir;
    private final String globalBinDir;
    private final String installed;
    private final boolean isDefault;
    private final String isDefaultMarker;
    private final String __action__;
    
    public ToolInstallationOutputDescriptor(String toolName, ToolDefinitionVersionDescriptor versionDescriptor, ToolInstallationDescriptor installationDescriptor, String action) {
        this(toolName, versionDescriptor, installationDescriptor, action, false);
    }
    
    public ToolInstallationOutputDescriptor(String toolName, ToolDefinitionVersionDescriptor versionDescriptor, ToolInstallationDescriptor installationDescriptor, String action, boolean isDefault) {
        this.name = toolName;
        this.version = versionDescriptor.getVersion();
        this.aliases = reverse(versionDescriptor.getAliases());
        this.aliasesString = String.join(", ", aliases);
        this.stable = versionDescriptor.isStable()?"Yes":"No";
        this.binaries = versionDescriptor.getBinaries();
        this.extraProperties = versionDescriptor.getExtraProperties();
        this.installDir = installationDescriptor==null ? null : installationDescriptor.getInstallDir();
        this.binDir = installationDescriptor==null ? null : installationDescriptor.getBinDir();
        this.globalBinDir = installationDescriptor==null ? null : installationDescriptor.getGlobalBinDir();
        this.installed = StringUtils.isBlank(this.installDir) ? "No" : "Yes";
        this.isDefault = isDefault;
        this.isDefaultMarker = isDefault ? "*" : "";
        this.__action__ = action;
    }
    
    private static final String[] reverse(String[] array) {
        if (array == null) {
            return new String[0];
        }
        List<String> list = Arrays.asList(array);
        Collections.reverse(list);
        return list.toArray(String[]::new);
    }
}
