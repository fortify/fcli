package com.fortify.cli.ssc.token.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

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
public class SSCTokenCommands extends AbstractFortifyCLICommand {
}
