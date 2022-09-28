package com.fortify.cli.ssc.user.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "user",
        subcommands = {
                SSCAuthEntityDeleteCommand.class,
                SSCAuthEntityGetCommand.class,
                SSCAuthEntityListCommand.class
        }
)
public class SSCAuthEntityCommands {
}
