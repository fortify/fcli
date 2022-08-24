package com.fortify.cli.sc_sast._main.cli;

import com.fortify.cli.sc_sast.ping.cli.SCSastPingCommand;
import com.fortify.cli.sc_sast.pkg.cli.SCSastPackageCommands;
import com.fortify.cli.sc_sast.rest.cli.SCSastRestCommand;
import com.fortify.cli.sc_sast.scan.cli.SCSastScanCommands;
import com.fortify.cli.sc_sast.sensor.cli.SCSastSensorCommands;
import com.fortify.cli.sc_sast.session.cli.SCSastSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "sc-sast",
        resourceBundle = "com.fortify.cli.sc_sast.i18n.SCSastMessages",
        subcommands = {
        		SCSastSessionCommands.class,
        		SCSastPingCommand.class,
                SCSastPackageCommands.class,
                SCSastRestCommand.class,
                SCSastScanCommands.class,
                SCSastSensorCommands.class
        }
)
public class SCSastCommands {
	public SCSastCommands(){
		System.setProperty("productName", "ScanCentral SAST");
	}
}
