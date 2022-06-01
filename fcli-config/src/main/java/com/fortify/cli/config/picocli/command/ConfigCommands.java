package com.fortify.cli.config.picocli.command;

import com.fortify.cli.config.picocli.command.autocomplete.AutoCompleteGenerationCommand;

import picocli.CommandLine.Command;

@Command(
        name = "config",
        description = "Commands for configuring fcli and its runtime environment.",
        subcommands = {
                AutoCompleteGenerationCommand.class
        }
)
public class ConfigCommands {
}
