package com.fortify.cli.fod.dast_scan.cli.cmd;

import com.fortify.cli.common.variable.PredefinedVariable;
import picocli.CommandLine;

@CommandLine.Command(name = "dast",
        aliases = {"dast-scan"},
        subcommands = {
                FoDDastScanStartCommand.class,
                FoDDastScanCancelCommand.class,
                FoDDastScanGetCommand.class,
                FoDDastScanListCommand.class,
                FoDDastScanImportCommand.class,
                FoDDastScanWaitForCommand.class
        }
)
@PredefinedVariable(name = "_fod_currentScan", field = "scanId")
public class FoDDastScanCommands {
}
