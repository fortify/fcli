package com.fortify.cli.config.truststore.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "truststore",
        description = "Commands for managing the trust store for SSL connections",
        subcommands = {
            TrustStoreClearCommand.class,
            TrustStoreGetCommand.class,
            TrustStoreSetCommand.class,
        }
)
public class TrustStoreCommands extends AbstractFortifyCLICommand {
}
