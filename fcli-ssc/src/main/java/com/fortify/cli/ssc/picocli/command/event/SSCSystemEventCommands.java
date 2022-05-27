package com.fortify.cli.ssc.picocli.command.event;

import picocli.CommandLine.Command;

@Command(
        name = "system-event",
        description = "Commands for interacting with system events on Fortify SSC.",
        subcommands = {
                SSCSystemEventListCommand.class
        }
)
public class SSCSystemEventCommands {
}
