package com.fortify.cli.sc_sast.scan.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.PredefinedVariable;

import picocli.CommandLine.Command;

@Command(
        name = "scan",
        subcommands = {
                SCSastControllerScanCancelCommand.class,
                SCSastControllerScanStartCommand.class,
                SCSastControllerScanStatusCommand.class,
                SCSastControllerScanWaitForCommand.class
        }
)
@PredefinedVariable(name = "_scsast_currentScan", field = "jobToken")
public class SCSastScanCommands extends AbstractFortifyCLICommand {
}
