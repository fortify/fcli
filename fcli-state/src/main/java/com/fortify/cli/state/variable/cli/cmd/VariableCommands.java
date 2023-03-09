package com.fortify.cli.state.variable.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

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
public class VariableCommands extends AbstractFortifyCLICommand {
}
