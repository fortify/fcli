package com.fortify.cli.sc_dast.scan.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "scan",
        subcommands = {
                SCDastScanCompleteOldCommand.class,
                SCDastScanDeleteOldCommand.class,
                SCDastScanGetCommand.class,
                SCDastScanListCommand.class,
                SCDastScanPauseOldCommand.class,
                SCDastScanResumeOldCommand.class,
                SCDastScanStartOldCommand.class,
                SCDastScanStatusOldCommand.class
        }
)
public class SCDastScanCommands {
}
