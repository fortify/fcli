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

import java.nio.file.Path;
import java.util.Set;

import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.SemVerHelper;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;

public final class ToolInstallationHelper {
    public static final String getResourcePath(String subPath) {
        return String.format("com/fortify/cli/tool/%s", subPath);
    }
    
    public static final String getResourcePath(String toolName, String subPath) {
        return getResourcePath(String.format("%s/%s", toolName.replace("-", "_"), subPath));
    }
    
    public static final Path getToolsStatePath() {
        return FcliDataHelper.getFcliStatePath().resolve("tools");
    }
    
    /**
     * The given version descriptor is considered a candidate for uninstall
     * if an installation descriptor for the version exists, and any of the 
     * following conditions are met:
     * - The full version is listed in --uninstall, optionally prefixed with 'v'
     * - The major version is listed in --uninstall, optionally prefixed with 'v'
     * - The major & minor version is listed in --uninstall, optionally prefixed with 'v'
     */
    public static final boolean isCandidateForUninstall(String toolName, Set<String> versionsToUninstall, ToolDefinitionVersionDescriptor versionDescriptor) {
        var version = versionDescriptor.getVersion();
        return (versionsToUninstall.contains("all") 
                || containsCandidateForUninstall(versionsToUninstall, version)
                || containsCandidateForUninstall(versionsToUninstall, SemVerHelper.getMajor(version).orElse("N/A"))
                || containsCandidateForUninstall(versionsToUninstall, SemVerHelper.getMajorMinor(version).orElse("N/A")))
               && ToolInstallationDescriptor.load(toolName, versionDescriptor)!=null; 
    }
    
    private static final boolean containsCandidateForUninstall(Set<String> versionsToUninstall, String candidate) {
        return versionsToUninstall.contains(candidate) || versionsToUninstall.contains("v"+candidate);
    }
}
