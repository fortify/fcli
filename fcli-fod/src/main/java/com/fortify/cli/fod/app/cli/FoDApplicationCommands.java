package com.fortify.cli.fod.app.cli;


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
