package com.fortify.cli.ssc.entity.user.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "user",
        subcommands = {
                SSCAuthEntityDeleteCommand.class,
                SSCAuthEntityGetCommand.class,
                SSCAuthEntityListCommand.class
        }
)
public class SSCAuthEntityCommands extends AbstractFortifyCLICommand {
}
