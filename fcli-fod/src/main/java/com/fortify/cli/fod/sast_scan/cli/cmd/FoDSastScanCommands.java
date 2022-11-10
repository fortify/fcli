package com.fortify.cli.fod.sast_scan.cli.cmd;

import picocli.CommandLine;

@CommandLine.Command(name = "sast",
        aliases = {"sast-scan"},
        subcommands = {
                FoDSastScanStartCommand.class,
                FoDSastScanCancelCommand.class,
                FoDSastScanGetCommand.class,
                FoDSastScanListCommand.class,
                FoDSastScanImportCommand.class
        }
)
public class FoDSastScanCommands {
}
