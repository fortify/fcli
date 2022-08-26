package com.fortify.cli.ssc.alert.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "alert",
        subcommands = {
                SSCAlertListCommand.class
        }
)
public class SSCAlertCommands {
}
