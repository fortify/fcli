package com.fortify.cli.sc_dast.scan_output.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "scan-output",
        subcommands = {
                SCDastScanOutputDownloadFprCommand.class,
                SCDastScanOutputDownloadLogsCommand.class,
                SCDastScanOutputGetResultsCommand.class,
                SCDastScanOutputPublishResultsCommand.class
        }
)
public class SCDastScanOutputCommands {
}
