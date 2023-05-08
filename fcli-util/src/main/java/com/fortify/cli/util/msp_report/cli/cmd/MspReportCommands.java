package com.fortify.cli.util.msp_report.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "msp-report",
        subcommands = {
            MspReportGenerateCommand.class,
            MspReportGenerateConfigCommand.class
        }
)
public class MspReportCommands extends AbstractFortifyCLICommand {}
