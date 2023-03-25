package com.fortify.cli.ssc.entity.event.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "event",
        subcommands = {
                SSCEventListCommand.class
        }
)
public class SSCEventCommands extends AbstractFortifyCLICommand {
}
