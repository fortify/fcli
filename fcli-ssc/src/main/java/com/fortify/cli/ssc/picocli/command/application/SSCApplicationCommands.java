package com.fortify.cli.ssc.picocli.command.application;

import picocli.CommandLine.Command;

@Command(
        name = "application",
        aliases = {"app"},
        description = "Commands for interacting with applications on Fortify SSC.",
        subcommands = {
                SSCApplicationListCommand.class
        }
)
public class SSCApplicationCommands {
}
