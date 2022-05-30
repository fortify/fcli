package com.fortify.cli.sc_dast.picocli.command.scan_settings;

import picocli.CommandLine.Command;

@Command(
        name = "scan-settings",
        description = "Manage ScanCentral DAST scan settings.",
        subcommands = {
             SCDastScanSettingsListCommand.class
        }
)
public class SCDastScanSettingsCommands {
}
