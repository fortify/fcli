package com.fortify.cli.ssc.auth_entity.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "user",
        subcommands = {
                SSCAuthEntityListCommand.class
        }
)
public class SSCAuthEntityCommands {
}
