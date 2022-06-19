package com.fortify.cli.ssc.picocli.command.event;

import picocli.CommandLine.Command;

@Command(
        name = "system-event",
        subcommands = {
                SSCSystemEventListCommand.class
        }
)
public class SSCSystemEventCommands {
}
