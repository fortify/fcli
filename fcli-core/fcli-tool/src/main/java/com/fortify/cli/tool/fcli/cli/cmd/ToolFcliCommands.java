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
package com.fortify.cli.tool.fcli.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

@Command(
        name = ToolFcliCommands.TOOL_NAME,
        subcommands = {
                ToolFcliInstallCommand.class,
                ToolFcliListCommand.class,
                ToolFcliListPlatformsCommand.class,
                ToolFcliRunCommand.class,
                ToolFcliUninstallCommand.class
        }

)
public class ToolFcliCommands extends AbstractContainerCommand {
    static final String TOOL_NAME = "fcli";
}