package com.fortify.cli.sc_dast.scan.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanCompleteCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanDeleteCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanImportFindingsCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanPauseCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanPublishCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanResumeCommand;

import picocli.CommandLine.Command;

@Command(
        name = "scan",
        subcommands = {
                SCDastScanCompleteCommand.class,
                SCDastScanDeleteCommand.class,
                SCDastScanDownloadCommand.class,
                SCDastScanGetCommand.class,
                SCDastScanImportFindingsCommand.class,
                SCDastScanListCommand.class,
                SCDastScanPauseCommand.class,
                SCDastScanPublishCommand.class,
                SCDastScanResumeCommand.class,
                SCDastScanStartCommand.class,
                SCDastScanWaitForCommand.class
        }
)
@DefaultVariablePropertyName("id")
public class SCDastScanCommands extends AbstractFortifyCLICommand {
}
