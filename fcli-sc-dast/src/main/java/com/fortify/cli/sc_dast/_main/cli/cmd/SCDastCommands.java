package com.fortify.cli.sc_dast._main.cli.cmd;

import com.fortify.cli.sc_dast.scan.cli.cmd.SCDastScanCommands;
import com.fortify.cli.sc_dast.scan_output.cli.cmd.SCDastScanOutputCommands;
import com.fortify.cli.sc_dast.scan_settings.cli.cmd.SCDastScanSettingsCommands;
import com.fortify.cli.sc_dast.sensor.cli.cmd.SCDastSensorCommands;
import com.fortify.cli.sc_dast.session.cli.cmd.SCDastSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "sc-dast",
        resourceBundle = "com.fortify.cli.sc_dast.i18n.SCDastMessages",
        hidden = true,
        subcommands = {
                SCDastSessionCommands.class,
                SCDastScanCommands.class,
                SCDastScanOutputCommands.class,
                SCDastScanSettingsCommands.class,
                SCDastSensorCommands.class,
        }
)
public class SCDastCommands {}
