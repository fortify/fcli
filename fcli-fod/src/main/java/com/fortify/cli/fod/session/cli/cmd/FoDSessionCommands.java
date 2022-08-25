package com.fortify.cli.fod.session.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "session",
        subcommands = {
                FoDSessionListCommand.class,
                FoDSessionLoginCommand.class,
                FoDSessionLogoutCommand.class 
        }
)
public class FoDSessionCommands {
}
