package com.fortify.cli.sc_dast.scan_output.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "scan-output",
        subcommands = {
                SCDastScanOutputDownloadFprOldCommand.class,
                SCDastScanOutputDownloadLogsOldCommand.class,
                SCDastScanOutputGetResultsOldCommand.class,
                SCDastScanOutputPublishResultsOldCommand.class
        }
)
public class SCDastScanOutputCommands {
}
