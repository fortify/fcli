package com.fortify.cli.fod.session.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "session",
        subcommands = {
                FoDSessionListCommand.class,
                FoDSessionLoginCommand.class,
                FoDSessionLogoutCommand.class 
        }
)
public class FoDSessionCommands extends AbstractFortifyCLICommand {
}
