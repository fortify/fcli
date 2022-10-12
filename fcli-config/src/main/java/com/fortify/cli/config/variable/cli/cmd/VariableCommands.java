package com.fortify.cli.config.variable.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "variable",
        aliases = "var",
        description = "Commands for managing fcli variables",
        subcommands = {
                VariableDefinitionCommands.class,
                VariableContentsCommands.class
        }
)
public class VariableCommands {
}
