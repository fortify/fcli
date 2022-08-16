package com.fortify.cli.ssc.picocli.command.app;

import picocli.CommandLine.Command;

@Command(
        name = "app",
        subcommands = {
                SSCAppListCommand.class
        }
)
public class SSCAppCommands {
}
