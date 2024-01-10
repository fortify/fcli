package com.fortify.cli.tool.fcli.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

@Command(
        name = ToolFcliCommands.TOOL_NAME,
        subcommands = {
                ToolFcliInstallCommand.class,
                ToolFcliListCommand.class,
                ToolFcliUninstallCommand.class
        }

)
public class ToolFcliCommands extends AbstractContainerCommand {
    static final String TOOL_NAME = "fcli";
}