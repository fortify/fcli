package com.fortify.cli.fod.picocli.command.session;

import picocli.CommandLine.Command;

@Command(
        name = "session",
        description = "Commands to manage Fortify on Demand (FoD) sessions.",
        subcommands = {
        		FoDSessionListCommand.class,
                FoDSessionLoginCommand.class,
                FoDSessionLogoutCommand.class 
        }
)
public class FoDSessionCommands {
}
