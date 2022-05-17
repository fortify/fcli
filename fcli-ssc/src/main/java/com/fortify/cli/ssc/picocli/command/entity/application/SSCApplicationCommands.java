package com.fortify.cli.ssc.picocli.command.entity.application;

import picocli.CommandLine.Command;

@Command(
        name = "app",
        aliases = {"application"},
        description = "Commands for interacting with applications on Fortify SSC.",
        subcommands = {
                SSCListApplicationsCommand.Impl.class
        }
)
public class SSCApplicationCommands {
}
