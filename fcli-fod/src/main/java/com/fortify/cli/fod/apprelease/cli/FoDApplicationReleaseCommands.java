package com.fortify.cli.fod.apprelease.cli;


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
