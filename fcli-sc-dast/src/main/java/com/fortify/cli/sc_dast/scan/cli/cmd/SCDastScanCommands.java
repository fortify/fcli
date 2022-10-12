package com.fortify.cli.sc_dast.scan.cli.cmd;

import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanCompleteCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanDeleteCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanPauseCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanResumeCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.retry.SCDastScanRetryCommands;

import picocli.CommandLine.Command;

@Command(
        name = "scan",
        subcommands = {
                SCDastScanCompleteCommand.class,
                SCDastScanDeleteCommand.class,
                SCDastScanDownloadCommand.class,
                SCDastScanGetCommand.class,
                SCDastScanListCommand.class,
                SCDastScanPauseCommand.class,
                SCDastScanResumeCommand.class,
                SCDastScanRetryCommands.class,
                SCDastScanStartCommand.class
        }
)
public class SCDastScanCommands {
}
