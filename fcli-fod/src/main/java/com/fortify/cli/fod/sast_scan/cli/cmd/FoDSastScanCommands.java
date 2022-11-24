package com.fortify.cli.fod.sast_scan.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine;

@CommandLine.Command(name = "sast",
        aliases = {"sast-scan"},
        subcommands = {
                FoDSastScanStartCommand.class,
                FoDSastScanCancelCommand.class,
                FoDSastScanGetCommand.class,
                FoDSastScanListCommand.class,
                FoDSastScanImportCommand.class,
                FoDSastScanWaitForCommand.class
        }
)
public class FoDSastScanCommands extends AbstractFortifyCLICommand {
}
