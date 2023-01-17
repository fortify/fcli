package com.fortify.cli.config.ssl.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.config.ssl.truststore.cli.cmd.SSLTrustStoreCommands;

import picocli.CommandLine.Command;

@Command(
        name = "ssl",
        description = "Commands for managing SSL connections",
        subcommands = {
            SSLTrustStoreCommands.class
        }
)
public class SSLCommands extends AbstractFortifyCLICommand {
}
