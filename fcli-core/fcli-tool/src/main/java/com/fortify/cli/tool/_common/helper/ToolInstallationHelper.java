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

import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;

public final class ToolInstallationHelper {
    public static final String getToolResourceDir(String toolName) {
        return String.format("com/fortify/cli/tool/%s", toolName);
    }
    
    public static final String getToolResourceFile(String toolName, String fileName) {
        return String.format("%s/%s", getToolResourceDir(toolName.replace('-', '_')), fileName);
    }
    
    public static final void uninstall(String toolName, ToolDefinitionVersionDescriptor versionDescriptor, ToolInstallationDescriptor installationDescriptor) {
        var installPath = installationDescriptor.getInstallPath();
        try {
            FileUtils.deleteRecursive(installPath);
        } finally {
            ToolInstallationDescriptor.delete(toolName, versionDescriptor);
        }
    }
}