package com.fortify.cli.fod.sast_scan.cli.cmd;

import com.fortify.cli.common.variable.PredefinedVariable;
import picocli.CommandLine;

@CommandLine.Command(name = "sast",
        aliases = {"sast-scan"},
        subcommands = {
                FoDSastScanStartCommand.class,
                FoDSastScanCancelCommand.class,
                FoDSastScanGetCommand.class,
                FoDSastScanListCommand.class,
                FoDSastScanImportCommand.class,
                FoDSastScanWaitForCommand.class
        }
)
@PredefinedVariable(name = "_fod_currentSastScan", field = "scanId")
public class FoDSastScanCommands {
}
