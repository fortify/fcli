package com.fortify.cli.config.picocli.command;

import com.fortify.cli.config.picocli.command.autocomplete.AutoCompleteGenerationCommand;

import com.fortify.cli.config.picocli.command.language.LanguageCommands;
import picocli.CommandLine.Command;

@Command(
        name = "config",
        description = "Commands for configuring fcli and its runtime environment.",
        resourceBundle = "com.fortify.cli.config.i18n.ConfigMessages",
        subcommands = {
                AutoCompleteGenerationCommand.class,
                LanguageCommands.class
        }
)
public class ConfigCommands {
}
