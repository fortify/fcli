package com.fortify.cli.config.ssl.truststore.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "truststore",
        description = "Commands for managing the trust store for SSL connections",
        subcommands = {
            SSLTrustStoreClearCommand.class,
            SSLTrustStoreGetCommand.class,
            SSLTrustStoreSetCommand.class,
        }
)
public class SSLTrustStoreCommands extends AbstractFortifyCLICommand {
}
