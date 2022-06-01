package com.fortify.cli.sc_sast.picocli.command;

import com.fortify.cli.sc_sast.picocli.command.pkg.SCSASTPackageCommands;
import com.fortify.cli.sc_sast.picocli.command.scan.SCSastScanCommands;
import com.fortify.cli.sc_sast.picocli.command.sensor.SCSastSensorCommands;
import com.fortify.cli.sc_sast.picocli.command.session.SCSastSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "sc-sast",
        description = "Commands for interacting with Fortify ScanCentral SAST.",
        subcommands = {
        		SCSastSessionCommands.class,
        		PingCommand.class,
                SCSASTPackageCommands.class,
                SCSastScanCommands.class,
                SCSastSensorCommands.class
        }
)
public class SCSastCommands {
}
