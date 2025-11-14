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

import com.fortify.cli.tool._common.cli.cmd.AbstractToolEnvCommand;
import com.fortify.cli.tool._common.helper.ToolPlatformHelper;

import picocli.CommandLine.Command;

@Command(name = "env")
public class ToolFoDUploaderEnvCommand extends AbstractToolEnvCommand {
    
    @Override
    protected String getToolName() {
        return ToolFoDUploaderCommands.TOOL_NAME;
    }
    
    @Override
    protected String[] getToolEnvVarPrefixes() {
        return ToolFoDUploaderCommands.TOOL_ENV_VAR_PREFIXES;
    }
    
    @Override
    protected String getDefaultBinaryName() {
        if (ToolPlatformHelper.isWindows()) {
            return "FoDUpload.bat";
        }
        return "FoDUpload";
    }
}
