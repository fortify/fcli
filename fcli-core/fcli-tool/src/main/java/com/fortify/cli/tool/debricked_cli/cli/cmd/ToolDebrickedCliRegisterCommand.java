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
package com.fortify.cli.tool.debricked_cli.cli.cmd;

import java.io.File;

import com.fortify.cli.tool._common.cli.cmd.AbstractToolRegisterCommand;
import com.fortify.cli.tool._common.helper.ToolPlatformHelper;
import com.fortify.cli.tool._common.helper.ToolVersionDetector;

import picocli.CommandLine.Command;

@Command(name = "register")
public class ToolDebrickedCliRegisterCommand extends AbstractToolRegisterCommand {
    
    @Override
    protected String getToolName() {
        return ToolDebrickedCliCommands.TOOL_NAME;
    }
    
    @Override
    protected String getDefaultBinaryName() {
        if (ToolPlatformHelper.isWindows()) {
            return "debricked.exe";
        }
        return "debricked";
    }
    
    @Override
    protected String detectVersion(File toolBinary, File installDir) {
        // Try executing debricked --version
        String output = ToolVersionDetector.tryExecute(toolBinary, "--version");
        if (output != null) {
            String version = ToolVersionDetector.extractVersionFromOutput(output);
            if (version != null) {
                return version;
            }
        }
        
        return "unknown";
    }
}
