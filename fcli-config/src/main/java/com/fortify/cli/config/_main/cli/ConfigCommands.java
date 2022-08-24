package com.fortify.cli.config._main.cli;

import com.fortify.cli.config.autocomplete.cli.AutoCompleteGenerationCommand;
import com.fortify.cli.config.language.cli.LanguageCommands;

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
