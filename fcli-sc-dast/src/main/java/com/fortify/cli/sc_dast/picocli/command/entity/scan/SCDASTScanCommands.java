package com.fortify.cli.sc_dast.picocli.command.entity.scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine.Command;

@Command(
        name = "scan",
        description = "Prepare, run and manage ScanCentral DAST scans.",
        subcommands = {
                SCDastScanCompleteCommand.class,
                SCDastScanDeleteCommand.class,
                SCDastScanPauseCommand.class,
                SCDastScanPublishCommand.class,
                SCDastScanResumeCommand.class,
                SCDastScanStartCommand.class
        }
)
public class SCDASTScanCommands {
}
