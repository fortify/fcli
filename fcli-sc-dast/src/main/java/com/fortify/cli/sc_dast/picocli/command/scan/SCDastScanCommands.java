package com.fortify.cli.sc_dast.picocli.command.scan;

import picocli.CommandLine.Command;

@Command(
        name = "scan",
        description = "Prepare, run and manage ScanCentral DAST scans and results.",
        subcommands = {
                SCDastScanCompleteCommand.class,
                SCDastScanDeleteCommand.class,
                SCDastScanListCommand.class,
                SCDastScanPauseCommand.class,
                SCDastScanResumeCommand.class,
                SCDastScanStartCommand.class,
                SCDastScanStatusCommand.class
        }
)
public class SCDastScanCommands {
}
