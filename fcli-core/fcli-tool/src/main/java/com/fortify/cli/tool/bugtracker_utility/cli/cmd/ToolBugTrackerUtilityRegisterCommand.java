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

import java.io.File;

import com.fortify.cli.tool._common.cli.cmd.AbstractToolRegisterCommand;
import com.fortify.cli.tool._common.helper.Tool;

import picocli.CommandLine.Command;

@Command(name = "register")
public class ToolBugTrackerUtilityRegisterCommand extends AbstractToolRegisterCommand {
    
    @Override
    protected final Tool getTool() {
        return Tool.BUGTRACKER_UTILITY;
    }
    
    @Override
    protected String detectVersion(File toolBinary, File installDir) {
        // BugTracker Utility: No version flag, no version in filename, no version in manifest
        // Only option is to rely on fcli install descriptor or return unknown for external installations
        return "unknown";
    }
}
