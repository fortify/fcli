package com.fortify.cli.state.variable.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "contents",
        description = "Commands for managing fcli variable contents",
        subcommands = {
                VariableContentsGetCommand.class,
                VariableContentsListCommand.class
        }
)
public class VariableContentsCommands extends AbstractFortifyCLICommand {
}
