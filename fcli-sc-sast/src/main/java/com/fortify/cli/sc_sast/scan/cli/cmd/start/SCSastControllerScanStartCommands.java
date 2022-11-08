package com.fortify.cli.sc_sast.scan.cli.cmd.start;

import com.fortify.cli.common.variable.PredefinedVariable;

import picocli.CommandLine.Command;

@Command(
        name = "start",
        subcommands = {
                SCSastControllerStartPackageScanCommand.class,
                SCSastControllerStartMbsScanCommand.class
        }
)
@PredefinedVariable(name = "currentScan", field = "jobToken")
public class SCSastControllerScanStartCommands {
}
