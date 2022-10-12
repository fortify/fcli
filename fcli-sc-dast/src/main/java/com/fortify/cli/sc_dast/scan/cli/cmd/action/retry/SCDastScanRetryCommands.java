package com.fortify.cli.sc_dast.scan.cli.cmd.action.retry;

import picocli.CommandLine.Command;

@Command(
        name = "retry",
        subcommands = {
                SCDastScanRetryImportResultsCommand.class,
                SCDastScanRetryImportFindingsCommand.class
        }
)
public class SCDastScanRetryCommands {
}
