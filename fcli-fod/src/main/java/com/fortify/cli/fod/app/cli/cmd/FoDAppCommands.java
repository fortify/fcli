package com.fortify.cli.fod.app.cli.cmd;


import picocli.CommandLine;

@CommandLine.Command(name = "app",
        aliases = {"application"},
        subcommands = {
                FoDAppCreateCommand.class,
                FoDAppListCommand.class,
                FoDAppUpdateCommand.class,
                FoDAppDeleteCommand.class
        }
)
public class FoDAppCommands {
}
