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
package com.fortify.cli.tool.fod_uploader.cli.cmd;

import com.fortify.cli.tool._common.cli.cmd.AbstractToolRegisterCommand;
import com.fortify.cli.tool._common.helper.ToolPlatformHelper;
import com.fortify.cli.tool._common.helper.ToolVersionDetector;

import picocli.CommandLine.Command;

@Command(name = "register")
public class ToolFoDUploaderRegisterCommand extends AbstractToolRegisterCommand {
    
    @Override
    protected String getToolName() {
        return ToolFoDUploaderCommands.TOOL_NAME;
    }
    
    @Override
    protected String getDefaultBinaryName() {
        if (ToolPlatformHelper.isWindows()) {
            return "FoDUpload.bat";
        }
        return "FoDUpload";
    }
    
    @Override
    protected String getToolEnvVarName() {
        return "FOD_UPLOADER";
    }
    
    @Override
    protected String getToolHomeEnvVarName() {
        return "FOD_UPLOADER_HOME";
    }
    
    @Override
    protected String detectVersion(java.io.File toolBinary, java.io.File installDir) {
        // FoD Uploader: Check JAR manifest for Implementation-Version
        // FodUpload.jar has no version in filename but has Implementation-Version in manifest
        String versionFromJar = ToolVersionDetector.extractVersionFromJarPattern(installDir, "FodUpload.jar", 1);
        if (versionFromJar != null) {
            return versionFromJar;
        }
        
        return "unknown";
    }
}
