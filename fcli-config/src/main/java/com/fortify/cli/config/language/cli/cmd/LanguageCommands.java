package com.fortify.cli.config.language.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

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
public class LanguageCommands extends AbstractFortifyCLICommand {
}
