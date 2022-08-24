package com.fortify.cli.sc_dast.scan_settings.cli;

import picocli.CommandLine.Command;

@Command(
        name = "scan-settings",
        subcommands = {
             SCDastScanSettingsListCommand.class
        }
)
public class SCDastScanSettingsCommands {
}
