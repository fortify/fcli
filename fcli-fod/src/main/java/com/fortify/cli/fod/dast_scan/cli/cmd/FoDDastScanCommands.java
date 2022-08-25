package com.fortify.cli.fod.dast_scan.cli.cmd;

import picocli.CommandLine;

@CommandLine.Command(name = "dast",
        aliases = {"dast-scan"},
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
