package com.fortify.cli.sc_dast.picocli.command.scan_settings;

import picocli.CommandLine.Command;

@Command(
        name = "scan-settings",
        subcommands = {
             SCDastScanSettingsListCommand.class
        }
)
public class SCDastScanSettingsCommands {
}
