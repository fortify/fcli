package com.fortify.cli.sc_sast.scan.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "scan",
        subcommands = {
                SCSastControllerScanCancelCommand.class,
                SCSastControllerScanStatusCommand.class,
                SCSastControllerScanWaitForArtifactCommand.class,
                SCSastControllerScanWaitForScanCommand.class,
                SCSastControllerScanWaitForUploadCommand.class,
        }
)
public class SCSastScanCommands {
}
