package com.fortify.cli.sc_sast.scan.cli.cmd;

import com.fortify.cli.common.variable.PredefinedVariable;

import picocli.CommandLine.Command;

@Command(
        name = "scan",
        subcommands = {
                SCSastControllerStartPackageScanCommand.class,
                SCSastControllerStartMbsScanCommand.class,
                SCSastControllerScanCancelCommand.class,
                SCSastControllerScanStatusCommand.class,
                SCSastControllerScanWaitForArtifactCommand.class,
                SCSastControllerScanWaitForScanCommand.class,
                SCSastControllerScanWaitForUploadCommand.class,
        }
)
@PredefinedVariable(name = "currentScan", field = "jobToken")
public class SCSastScanCommands {
}
