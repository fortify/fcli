package com.fortify.cli.sc_sast.scan.cli;

import picocli.CommandLine.Command;

@Command(
        name = "scan",
        subcommands = {
                SCSastScanCancelCommand.class,
                SCSastScanStartCommand.class,
                SCSastScanStatusCommand.class
        }
)
public class SCSastScanCommands {
}
