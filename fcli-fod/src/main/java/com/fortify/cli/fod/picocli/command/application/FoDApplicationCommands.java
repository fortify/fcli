package com.fortify.cli.fod.picocli.command.application;


import picocli.CommandLine;

@CommandLine.Command(name = "app",
        aliases = {"application"},
        subcommands = {
                FoDApplicationCreateCommand.class,
                FoDApplicationListCommand.class,
                FoDApplicationUpdateCommand.class,
                FoDApplicationDeleteCommand.class
        }
)
public class FoDApplicationCommands {
}
