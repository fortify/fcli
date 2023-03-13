package com.fortify.cli.state.variable.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "variable",
        aliases = "var",
        description = "Commands for managing fcli variables",
        subcommands = {
                VariableContentsCommand.class,
                VariableDeleteAllCommand.class,
                VariableDeleteCommand.class,
                VariableGetCommand.class,
                VariableListCommand.class
                
        }
)
public class VariableCommands extends AbstractFortifyCLICommand {
}
