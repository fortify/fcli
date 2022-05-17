package com.fortify.cli.fod.command.entity.sast_scan;

import picocli.CommandLine;

@CommandLine.Command(name = "sast",
        aliases = {"sast-scan"},
        description = "Commands for interacting with SAST scans on FoD.",
        subcommands = {
                FODSASTScanCreateCommand.class,
                FODSASTScanGetCommand.class,
                FODSASTScanListCommand.class,
                FODSASTScanUpdateCommand.class,
                FODSASTScanDeleteCommand.class
        }
)
public class FODSASTScanCommands {
}
