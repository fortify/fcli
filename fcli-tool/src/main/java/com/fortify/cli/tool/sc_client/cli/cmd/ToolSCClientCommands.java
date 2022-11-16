package com.fortify.cli.tool.sc_client.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "sc-client",
        aliases = {"scancentral-client"},
        subcommands = {
                ToolSCClientInstallCommand.class
        }

)
public class ToolSCClientCommands {}
