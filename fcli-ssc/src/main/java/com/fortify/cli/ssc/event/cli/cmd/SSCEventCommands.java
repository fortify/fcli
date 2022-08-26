package com.fortify.cli.ssc.event.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "event",
        subcommands = {
                SSCEventListCommand.class
        }
)
public class SSCEventCommands {
}
