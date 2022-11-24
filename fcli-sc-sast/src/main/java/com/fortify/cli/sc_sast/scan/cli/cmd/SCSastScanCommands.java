package com.fortify.cli.sc_sast.scan.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.PredefinedVariable;
import com.fortify.cli.sc_sast.scan.cli.cmd.start.SCSastControllerScanStartCommands;

import picocli.CommandLine.Command;

@Command(
        name = "scan",
        subcommands = {
                SCSastControllerScanCancelCommand.class,
                SCSastControllerScanStartCommands.class,
                SCSastControllerScanStatusCommand.class,
                SCSastControllerScanWaitForCommand.class
        }
)
@PredefinedVariable(name = "_scsast_currentScan", field = "jobToken")
public class SCSastScanCommands extends AbstractFortifyCLICommand {
}
