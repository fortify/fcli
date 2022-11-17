package com.fortify.cli.tool.sc_client.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = ToolSCClientCommands.TOOL_NAME,
        aliases = {"scancentral-client"},
        subcommands = {
                ToolSCClientInstallCommand.class,
                ToolSCClientListCommand.class
        }

)
public class ToolSCClientCommands {
    static final String TOOL_NAME = "sc-client";
}