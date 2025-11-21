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

import java.io.File;

import com.fortify.cli.tool._common.cli.cmd.AbstractToolRegisterCommand;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolVersionDetector;

import picocli.CommandLine.Command;

@Command(name = "register")
public class ToolSCClientRegisterCommand extends AbstractToolRegisterCommand {
    
    @Override
    protected final Tool getTool() {
        return Tool.SC_CLIENT;
    }
    
    @Override
    protected String detectVersion(File toolBinary, File installDir) {
        // Try JAR filename detection first (faster than executing binary)
        String versionFromFilename = ToolVersionDetector
            .extractVersionFromJarFilename(installDir, "Core/lib/scancentral-cli-{version}.jar", 3);
        if (versionFromFilename != null) {
            return versionFromFilename;
        }
        
        // Fallback: Execute scancentral -version
        String output = ToolVersionDetector.tryExecute(toolBinary, "-version");
        if (output != null) {
            String version = ToolVersionDetector.extractVersionFromOutput(output);
            if (version != null) {
                return version;
            }
        }
        
        return "unknown";
    }
}
