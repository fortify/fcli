package com.fortify.cli.fod.apprelease.cli.cmd;


import picocli.CommandLine;

@CommandLine.Command(name = "release",
        aliases = {"application-release"},
        subcommands = {
                FoDAppReleaseCreateCommand.class,
                FoDAppReleaseListCommand.class,
                FoDAppReleaseUpdateCommand.class,
                FoDAppReleaseDeleteCommand.class
        }
)
public class FoDReleaseCommands {
}
