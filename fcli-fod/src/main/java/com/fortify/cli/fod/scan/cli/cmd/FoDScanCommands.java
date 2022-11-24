package com.fortify.cli.fod.scan.cli.cmd;

import com.fortify.cli.common.variable.PredefinedVariable;
import picocli.CommandLine;

@CommandLine.Command(name = "scan",
        subcommands = {
                FoDScanCancelCommand.class,
                FoDScanGetCommand.class,
                FoDScanListCommand.class,
                FoDScanWaitForCommand.class
        }
)
@PredefinedVariable(name = "_fod_currentScan", field = "id")
public class FoDScanCommands {
}
