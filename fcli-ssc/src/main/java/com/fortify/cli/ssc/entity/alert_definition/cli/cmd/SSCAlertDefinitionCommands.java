package com.fortify.cli.ssc.entity.alert_definition.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "alert-definition",
        subcommands = {
                SSCAlertDefinitionGetCommand.class,
                SSCAlertDefinitionListCommand.class
        }
)
public class SSCAlertDefinitionCommands extends AbstractFortifyCLICommand {
}
