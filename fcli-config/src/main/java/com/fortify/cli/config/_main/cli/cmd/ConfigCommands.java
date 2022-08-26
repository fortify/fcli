package com.fortify.cli.config._main.cli.cmd;

import com.fortify.cli.config.autocomplete.cli.cmd.AutoCompleteGenerationCommand;
import com.fortify.cli.config.language.cli.cmd.LanguageCommands;

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
