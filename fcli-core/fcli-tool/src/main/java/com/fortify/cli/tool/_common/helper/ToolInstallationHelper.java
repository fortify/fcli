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
    public static final String getToolResourceLocation(String subPath) {
        return String.format("com/fortify/cli/tool/%s", subPath);
    }
    
    public static final Path getToolsStatePath() {
        return FcliDataHelper.getFcliStatePath().resolve("tools");
    }
    
    /**
     * The given version descriptor is considered a candidate for uninstall
     * if all of the following conditions are met:
     * - The full version is listed in --uninstall
     * - The major version is listed in --uninstall
     * - The major & minor version is listed in --uninstall
     * - An installation descriptor for the version exists
     */
    public static final boolean isCandidateForUninstall(String toolName, Set<String> versionsToUninstall, ToolDefinitionVersionDescriptor versionDescriptor) {
        var version = versionDescriptor.getVersion();
        return (versionsToUninstall.contains("all") 
                || versionsToUninstall.contains(version)
                || versionsToUninstall.contains(SemVerHelper.getMajor(version).orElse("N/A"))
                || versionsToUninstall.contains(SemVerHelper.getMajorMinor(version).orElse("N/A")))
               && ToolInstallationDescriptor.load(toolName, versionDescriptor)!=null; 
    }
}
