package com.fortify.cli.state.variable.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "definition",
        aliases = "def",
        description = "Commands for managing fcli variable definitions",
        subcommands = {
                VariableDefinitionClearCommand.class,
                VariableDefinitionDeleteCommand.class,
                VariableDefinitionGetCommand.class,
                VariableDefinitionListCommand.class
                
        }
)
public class VariableDefinitionCommands extends AbstractFortifyCLICommand {
}
