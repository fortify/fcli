package com.fortify.cli.fod.picocli.command.application.release;


import picocli.CommandLine;

@CommandLine.Command(name = "release",
        aliases = {"application-release"},
        description = "Commands for interacting with application releases on FoD.",
        subcommands = {
                FoDApplicationReleaseCreateCommand.class,
                FoDApplicationReleaseListCommand.class,
                FoDApplicationUpdateCommand.class,
                FoDApplicationReleaseDeleteCommand.class
        }
)
public class FoDApplicationReleaseCommands {
}
