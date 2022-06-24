package com.fortify.cli.ssc.picocli.command.event;

import picocli.CommandLine.Command;

@Command(
        name = "event",
        subcommands = {
                SSCEventListCommand.class
        }
)
public class SSCEventCommands {
}
