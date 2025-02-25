package com.fortify.cli.aviator.token.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import picocli.CommandLine.Command;

@Command(
        name = "token",
        subcommands = {
                AviatorTokenCreateCommand.class,
                AviatorTokenDeleteCommand.class,
                AviatorTokenListCommand.class,
                AviatorTokenRevokeCommand.class,
                AviatorTokenValidateCommand.class
        }
)
public class AviatorTokenCommands extends AbstractContainerCommand {
}
