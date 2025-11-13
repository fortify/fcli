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
package com.fortify.cli.tool.bugtracker_utility.cli.cmd;

import com.fortify.cli.tool._common.cli.cmd.AbstractToolRegisterCommand;
import com.fortify.cli.tool._common.helper.ToolPlatformHelper;

import picocli.CommandLine.Command;

@Command(name = "register")
public class ToolBugTrackerUtilityRegisterCommand extends AbstractToolRegisterCommand {
    
    @Override
    protected String getToolName() {
        return ToolBugTrackerUtilityCommands.TOOL_NAME;
    }
    
    @Override
    protected String getDefaultBinaryName() {
        if (ToolPlatformHelper.isWindows()) {
            return "FortifyBugTrackerUtility.bat";
        }
        return "FortifyBugTrackerUtility";
    }
    
    @Override
    protected String[] getToolEnvVarPrefixes() {
        return new String[]{"BUGTRACKER_UTILITY", "FBTU"};
    }
    
    @Override
    protected String detectVersion(java.io.File toolBinary, java.io.File installDir) {
        // BugTracker Utility: No version flag, no version in filename, no version in manifest
        // Only option is to rely on fcli install descriptor or return unknown for external installations
        return "unknown";
    }
}
