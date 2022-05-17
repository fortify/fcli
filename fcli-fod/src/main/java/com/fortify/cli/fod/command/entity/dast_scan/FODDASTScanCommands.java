package com.fortify.cli.fod.command.entity.dast_scan;

import picocli.CommandLine;

@CommandLine.Command(name = "dast",
        aliases = {"dast-scan"},
        description = "Commands for interacting with DAST scans on FoD.",
        subcommands = {
                FODDASTScanCreateCommand.class,
                FODDASTScanGetCommand.class,
                FODDASTScanListCommand.class,
                FODDASTScanUpdateCommand.class,
                FODDASTScanDeleteCommand.class
        }
)
public class FODDASTScanCommands {
}
