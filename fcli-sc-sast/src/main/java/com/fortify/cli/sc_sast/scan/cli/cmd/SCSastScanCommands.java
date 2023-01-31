package com.fortify.cli.sc_sast.scan.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;

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
@DefaultVariablePropertyName("jobToken")
public class SCSastScanCommands extends AbstractFortifyCLICommand {
}
