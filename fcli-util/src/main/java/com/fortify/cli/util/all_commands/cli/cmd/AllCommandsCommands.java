package com.fortify.cli.util.all_commands.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "all-commands",
        subcommands = {
            AllCommandsHelpCommand.class,
            AllCommandsListCommand.class
        }
)
public class AllCommandsCommands extends AbstractFortifyCLICommand {}
