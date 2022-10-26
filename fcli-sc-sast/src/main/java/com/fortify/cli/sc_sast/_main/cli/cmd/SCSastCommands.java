package com.fortify.cli.sc_sast._main.cli.cmd;

import com.fortify.cli.sc_sast.mbs.cli.cmd.SCSastMbsCommands;
import com.fortify.cli.sc_sast.ping.cli.cmd.SCSastPingCommand;
import com.fortify.cli.sc_sast.pkg.cli.cmd.SCSastPackageCommands;
import com.fortify.cli.sc_sast.rest.cli.cmd.SCSastControllerRestCommands;
import com.fortify.cli.sc_sast.scan.cli.cmd.SCSastScanCommands;
import com.fortify.cli.sc_sast.sensor.cli.cmd.SCSastSensorCommands;
import com.fortify.cli.sc_sast.session.cli.cmd.SCSastSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "sc-sast",
        resourceBundle = "com.fortify.cli.sc_sast.i18n.SCSastMessages",
        hidden = true,
        subcommands = {
                // This list of subcommands starts with generic session and rest commands,
                // followed by all entity commands in alphabetical order
                SCSastSessionCommands.class,
                SCSastControllerRestCommands.class,
                SCSastMbsCommands.class,
                SCSastPackageCommands.class,
                SCSastPingCommand.class,
                SCSastScanCommands.class,
                SCSastSensorCommands.class
        }
)
public class SCSastCommands {}
