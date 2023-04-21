package com.fortify.cli.util.ncd_report.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "ncd-report",
        subcommands = {
            NcdReportGenerateCommand.class,
            NcdReportGenerateConfigCommand.class
        }
)
public class NcdReportCommands extends AbstractFortifyCLICommand {}
