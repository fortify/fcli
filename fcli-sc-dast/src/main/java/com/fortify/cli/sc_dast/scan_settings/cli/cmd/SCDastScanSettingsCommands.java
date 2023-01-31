package com.fortify.cli.sc_dast.scan_settings.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;

import picocli.CommandLine.Command;

@Command(
        name = "scan-settings",
        subcommands = {
            SCDastScanSettingsGetCommand.class,
            SCDastScanSettingsListCommand.class
        }
)
@DefaultVariablePropertyName("id")
public class SCDastScanSettingsCommands extends AbstractFortifyCLICommand {
}
