package com.fortify.cli.ssc.picocli.command.app;

import picocli.CommandLine.Command;

@Command(
        name = "application",
        aliases = {"app"},
        subcommands = {
                SSCApplicationListCommand.class
        }
)
public class SSCApplicationCommands {
}
