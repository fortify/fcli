package com.fortify.cli.fod.command.entity.application;


import picocli.CommandLine;

@CommandLine.Command(name = "app",
        aliases = {"application"},
        description = "Commands for interacting with applications on FoD.",
        subcommands = {
                FODApplicationCreateCommand.class,
                FODApplicationGetCommand.class,
                FODApplicationListCommand.class,
                FODApplicationUpdateCommand.class,
                FODApplicationDeleteCommand.class
        }
)
public class FODApplicationCommands {
}
