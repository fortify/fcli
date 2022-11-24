package com.fortify.cli.sc_sast.scan.cli.cmd.start;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "start",
        subcommands = {
                SCSastControllerStartPackageScanCommand.class,
                SCSastControllerStartMbsScanCommand.class
        }
)
public class SCSastControllerScanStartCommands extends AbstractFortifyCLICommand {
}
