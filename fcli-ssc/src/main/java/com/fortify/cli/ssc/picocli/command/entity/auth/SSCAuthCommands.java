package com.fortify.cli.ssc.picocli.command.entity.auth;

import picocli.CommandLine.Command;

@Command(
        name = "auth",
        aliases = {"authentication"},
        description = "Commands related to authentication with Fortify SSC.",
        subcommands = {
                SSCLoginCommand.class,
                SSCLogoutCommand.class
        }
)
public class SSCAuthCommands {
}
