package com.fortify.cli.sc_sast.scan.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "scan",
        subcommands = {
                SCSastControllerScanCancelCommand.class,
                SCSastControllerScanStatusCommand.class
        }
)
public class SCSastScanCommands {
}
