package com.fortify.cli.ssc.app.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "app",
        subcommands = {
                SSCAppGetCommand.class,
                SSCAppListCommand.class
        }
)
public class SSCAppCommands {
}
