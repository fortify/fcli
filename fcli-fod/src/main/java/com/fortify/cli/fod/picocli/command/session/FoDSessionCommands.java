package com.fortify.cli.fod.picocli.command.session;

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
