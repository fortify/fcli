package com.fortify.cli.util.autocomplete.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "auto-complete",
        subcommands = {
            AutoCompleteGenerationCommand.class
        }
)
public class AutoCompleteCommands extends AbstractFortifyCLICommand {}
