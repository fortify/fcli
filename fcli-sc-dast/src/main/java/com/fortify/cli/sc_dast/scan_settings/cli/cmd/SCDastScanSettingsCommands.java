package com.fortify.cli.sc_dast.scan_settings.cli.cmd;

import com.fortify.cli.common.variable.MinusVariableDefinition;

import picocli.CommandLine.Command;

@Command(
        name = "scan-settings",
        subcommands = {
            SCDastScanSettingsGetCommand.class,
            SCDastScanSettingsListCommand.class
        }
)
@MinusVariableDefinition(name = "currentScanSettings", field = "id")
public class SCDastScanSettingsCommands {
}
