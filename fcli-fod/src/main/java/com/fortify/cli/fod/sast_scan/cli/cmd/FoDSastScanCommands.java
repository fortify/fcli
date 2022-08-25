package com.fortify.cli.fod.sast_scan.cli.cmd;

import picocli.CommandLine;

@CommandLine.Command(name = "sast",
        aliases = {"sast-scan"},
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
