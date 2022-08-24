package com.fortify.cli.config.language.cli;

import picocli.CommandLine;

@CommandLine.Command(
        name = "language",
        aliases = {"lang", "i18n"},
        subcommands = {
                LanguageListCommand.class,
                LanguageSetCommand.class,
                LanguageGetCommand.class
        }
)
public class LanguageCommands {
}
