package com.fortify.cli.ssc.entity.alert.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "alert",
        subcommands = {
                SSCAlertListCommand.class
        }
)
public class SSCAlertCommands extends AbstractFortifyCLICommand {
}
