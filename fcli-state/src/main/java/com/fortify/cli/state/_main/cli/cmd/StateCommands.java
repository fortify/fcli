package com.fortify.cli.state._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.state.variable.cli.cmd.VariableCommands;

import picocli.CommandLine.Command;

@Command(
        name = "state",
        resourceBundle = "com.fortify.cli.state.i18n.StateMessages",
        subcommands = {
                StateClearCommand.class,
                VariableCommands.class
        }
)
public class StateCommands extends AbstractFortifyCLICommand {
}
