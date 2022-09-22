package com.fortify.cli.ssc.app.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "app",
        subcommands = {
                SSCAppDeleteCommand.class,
                SSCAppGetCommand.class,
                SSCAppListCommand.class,
                SSCAppUpdateCommand.class
        }
)
public class SSCAppCommands {
}
