package com.fortify.cli.sc_sast.picocli.command;

import com.fortify.cli.sc_sast.picocli.command.pkg.SCSASTPackageCommands;
import com.fortify.cli.sc_sast.picocli.command.scan.SCSASTScanCommands;
import com.fortify.cli.sc_sast.picocli.command.sensor.SCSASTSensorCommands;

import picocli.CommandLine.Command;

@Command(
        name = "sc-sast",
        description = "Commands for interacting with Fortify ScanCentral SAST.",
        subcommands = {
                SCSASTPackageCommands.class,
                SCSASTScanCommands.class,
                SCSASTSensorCommands.class
        }
)
public class SCSASTCommands {
}
