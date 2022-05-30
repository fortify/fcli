package com.fortify.cli.fod.picocli.command.sast_scan;

import picocli.CommandLine;

@CommandLine.Command(name = "sast",
        aliases = {"sast-scan"},
        description = "Commands for interacting with SAST scans on FoD.",
        subcommands = {
                FoDSastScanCreateCommand.class,
                FoDSastScanGetCommand.class,
                FoDSastScanListCommand.class,
                FoDSastScanUpdateCommand.class,
                FoDSastScanDeleteCommand.class
        }
)
public class FoDSastScanCommands {
}
