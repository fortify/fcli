package com.fortify.cli.sc_dast.scan_settings.cli.cmd;

import com.fortify.cli.common.variable.PredefinedVariable;

import picocli.CommandLine.Command;

@Command(
        name = "scan-settings",
        subcommands = {
            SCDastScanSettingsGetCommand.class,
            SCDastScanSettingsListCommand.class
        }
)
@PredefinedVariable(name = "currentScanSettings", field = "id")
public class SCDastScanSettingsCommands {
}
