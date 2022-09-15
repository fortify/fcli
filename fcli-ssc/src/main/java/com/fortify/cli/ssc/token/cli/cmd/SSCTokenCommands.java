package com.fortify.cli.ssc.token.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "token",
        aliases = {},
        subcommands = {
                SSCTokenCreateCommand.class,
                SSCTokenListCommand.class,
                SSCTokenRevokeCommand.class,
                SSCTokenUpdateCommand.class
        }

)
public class SSCTokenCommands {
}
