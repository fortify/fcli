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
package com.fortify.cli.tool.sc_client.cli.cmd;


import com.fortify.cli.tool._common.cli.cmd.AbstractToolRegisterCommand;
import com.fortify.cli.tool._common.helper.ToolPlatformHelper;
import com.fortify.cli.tool._common.helper.ToolVersionDetector;

import picocli.CommandLine.Command;

@Command(name = "register")
public class ToolSCClientRegisterCommand extends AbstractToolRegisterCommand {
    
    @Override
    protected String getToolName() {
        return ToolSCClientCommands.TOOL_NAME;
    }
    
    @Override
    protected String getDefaultBinaryName() {
        if (ToolPlatformHelper.isWindows()) {
            return "scancentral.bat";
        }
        return "scancentral";
    }
    
    @Override
    protected String getToolEnvVarName() {
        return "SCANCENTRAL";
    }
    
    @Override
    protected String getToolHomeEnvVarName() {
        return "SCANCENTRAL_HOME";
    }
    
    @Override
    protected String detectVersion(java.io.File toolBinary, java.io.File installDir) {
        // Try executing scancentral -version
        String output = ToolVersionDetector.tryExecute(toolBinary, "-version");
        if (output != null) {
            String version = ToolVersionDetector.extractVersionFromOutput(output);
            if (version != null) {
                return version;
            }
        }
        
        // Fallback: extract version from Core/lib/scancentral-cli-{version}.jar (filename or manifest)
        String versionFromJar = ToolVersionDetector
            .extractVersionFromJarPattern(installDir, "Core/lib/scancentral-cli-{version}.jar", 3);
        if (versionFromJar != null) {
            return versionFromJar;
        }
        
        return "unknown";
    }
}
