package com.fortify.cli.ssc.alert_definition.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "alert-definition",
        subcommands = {
                SSCAlertDefinitionListCommand.class
        }
)
public class SSCAlertDefinitionCommands {
}
