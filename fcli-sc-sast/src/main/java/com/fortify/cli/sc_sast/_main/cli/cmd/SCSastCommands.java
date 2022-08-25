package com.fortify.cli.sc_sast._main.cli.cmd;

import com.fortify.cli.sc_sast.ping.cli.cmd.SCSastPingCommand;
import com.fortify.cli.sc_sast.pkg.cli.cmd.SCSastPackageCommands;
import com.fortify.cli.sc_sast.rest.cli.cmd.SCSastRestCommand;
import com.fortify.cli.sc_sast.scan.cli.cmd.SCSastScanCommands;
import com.fortify.cli.sc_sast.sensor.cli.cmd.SCSastSensorCommands;
import com.fortify.cli.sc_sast.session.cli.cmd.SCSastSessionCommands;

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
