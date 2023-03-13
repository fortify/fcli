package com.fortify.cli.config._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.config.language.cli.cmd.LanguageCommands;
import com.fortify.cli.config.proxy.cli.cmd.ProxyCommands;
import com.fortify.cli.config.truststore.cli.cmd.TrustStoreCommands;

import picocli.CommandLine.Command;

@Command(
        name = "config",
        aliases = "cfg",
        resourceBundle = "com.fortify.cli.config.i18n.ConfigMessages",
        subcommands = {
                ConfigClearCommand.class,
                LanguageCommands.class,
                ProxyCommands.class,
                TrustStoreCommands.class
        }
)
public class ConfigCommands extends AbstractFortifyCLICommand {
}
