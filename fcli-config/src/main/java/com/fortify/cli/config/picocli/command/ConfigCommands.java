package com.fortify.cli.config.picocli.command;

import com.fortify.cli.config.picocli.command.autocomplete.AutoCompleteGenerationCommand;

import com.fortify.cli.config.picocli.command.language.LanguageCommands;
import picocli.CommandLine.Command;

@Command(
        name = "config",
        description = "Commands for configuring fcli and its runtime environment.",
        subcommands = {
                AutoCompleteGenerationCommand.class,
                LanguageCommands.class
        },
        resourceBundle = "i18n_Config"
)
public class ConfigCommands {
}
