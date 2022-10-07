package com.fortify.cli.fod.apprelease.cli.cmd;


import picocli.CommandLine;

@CommandLine.Command(name = "release",
        aliases = {"application-release"},
        subcommands = {
                FoDAppReleaseCreateCommand.class,
                FoDAppReleaseHttpListCommand.class,
                FoDAppReleaseUpdateCommand.class,
                FoDAppReleaseDeleteCommand.class
        }
)
public class FoDReleaseCommands {
}
