package com.fortify.cli.ssc.app.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "app",
        subcommands = {
                SSCAppListCommand.class
        }
)
public class SSCAppCommands {
}
