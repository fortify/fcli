package com.fortify.cli.sc_dast.scan_settings.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "scan-settings",
        subcommands = {
            SCDastScanSettingsGetCommand.class,
            SCDastScanSettingsListCommand.class
        }
)
public class SCDastScanSettingsCommands {
}
