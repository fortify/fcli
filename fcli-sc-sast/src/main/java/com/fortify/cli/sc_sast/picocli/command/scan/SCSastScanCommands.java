package com.fortify.cli.sc_sast.picocli.command.scan;

import picocli.CommandLine.Command;

@Command(
        name = "scan",
        description = "Prepare, run and manage ScanCentral SAST scans and results.",
        subcommands = {
                SCSastScanCancelCommand.class,
                SCSastScanStartCommand.class,
                SCSastScanStatusCommand.class
        }
)
public class SCSastScanCommands {
}
