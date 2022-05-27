package com.fortify.cli.ssc.picocli.command.session;

import picocli.CommandLine.Command;

@Command(
        name = "session",
        description = "Commands to manage Fortify SSC sessions.",
        subcommands = {
                SSCSessionLoginCommand.class,
                SSCSessionLogoutCommand.class
        }
)
public class SSCSessionCommands {
}
