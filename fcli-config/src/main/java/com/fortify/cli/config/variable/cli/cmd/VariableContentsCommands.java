package com.fortify.cli.config.variable.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "contents",
        description = "Commands for managing fcli variable contents",
        subcommands = {
                VariableContentsGetCommand.class,
                VariableContentsListCommand.class
        }
)
public class VariableContentsCommands {
}
