package com.fortify.cli.tool.sc_client.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = ToolSCClientCommands.TOOL_NAME,
        aliases = {"scancentral-client"},
        subcommands = {
                ToolSCClientInstallCommand.class,
                ToolSCClientListCommand.class,
                ToolSCClientUninstallCommand.class
        }

)
public class ToolSCClientCommands extends AbstractFortifyCLICommand {
    static final String TOOL_NAME = "sc-client";
}