package com.fortify.cli.sc_sast.picocli.command;

import com.fortify.cli.sc_sast.picocli.command.pkg.SCSastPackageCommands;
import com.fortify.cli.sc_sast.picocli.command.scan.SCSastScanCommands;
import com.fortify.cli.sc_sast.picocli.command.sensor.SCSastSensorCommands;
import com.fortify.cli.sc_sast.picocli.command.session.SCSastSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "sc-sast",
        description = "Commands for interacting with Fortify ScanCentral SAST.",
        resourceBundle = "com.fortify.cli.sc_sast.i18n.SCSastMessages",
        subcommands = {
        		SCSastSessionCommands.class,
        		SCSastPingCommand.class,
                SCSastPackageCommands.class,
                SCSastScanCommands.class,
                SCSastSensorCommands.class
        }
)
public class SCSastCommands {
}
