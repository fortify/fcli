package com.fortify.cli.fod.picocli.command.dast_scan;

import picocli.CommandLine;

@CommandLine.Command(name = "dast",
        aliases = {"dast-scan"},
        description = "Commands for interacting with DAST scans on FoD.",
        subcommands = {
                FoDDastScanCreateCommand.class,
                FoDDastScanGetCommand.class,
                FoDDastScanListCommand.class,
                FoDDastScanUpdateCommand.class,
                FoDDastScanDeleteCommand.class
        }
)
public class FoDDastScanCommands {
}
