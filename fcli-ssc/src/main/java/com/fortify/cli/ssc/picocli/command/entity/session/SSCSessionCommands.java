package com.fortify.cli.ssc.picocli.command.entity.session;

import picocli.CommandLine.Command;

@Command(
        name = "auth",
        aliases = {"authentication"},
        description = "Commands related to authentication with Fortify SSC.",
        subcommands = {
                SSCSessionLoginCommand.class,
                SSCSessionLogoutCommand.class
        }
)
public class SSCSessionCommands {
}
