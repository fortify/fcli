package com.fortify.cli.fod.picocli.command.application.release;


import picocli.CommandLine;

@CommandLine.Command(name = "release",
        aliases = {"application-release"},
        subcommands = {
                FoDApplicationReleaseCreateCommand.class,
                FoDApplicationReleaseListCommand.class,
                FoDApplicationUpdateCommand.class,
                FoDApplicationReleaseDeleteCommand.class
        }
)
public class FoDApplicationReleaseCommands {
}
