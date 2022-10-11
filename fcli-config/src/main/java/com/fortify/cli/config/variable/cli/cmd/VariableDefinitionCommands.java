package com.fortify.cli.config.variable.cli.cmd;

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
public class VariableDefinitionCommands {
}
