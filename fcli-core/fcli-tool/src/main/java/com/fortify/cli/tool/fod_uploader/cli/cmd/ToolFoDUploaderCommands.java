/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.tool.fod_uploader.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

@Command(
        name = ToolFoDUploaderCommands.TOOL_NAME,
        aliases = {"fodupload"},
        subcommands = {
                ToolFoDUploaderInstallCommand.class,
                ToolFoDUploaderListCommand.class,
                ToolFoDUploaderListPlatformsCommand.class,
                ToolFoDUploaderUninstallCommand.class
        }

)
public class ToolFoDUploaderCommands extends AbstractContainerCommand {
    static final String TOOL_NAME = "fod-uploader";
}
