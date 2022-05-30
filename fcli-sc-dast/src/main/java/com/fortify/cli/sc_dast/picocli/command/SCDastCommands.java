package com.fortify.cli.sc_dast.picocli.command;

import com.fortify.cli.sc_dast.picocli.command.scan.SCDastScanCommands;
import com.fortify.cli.sc_dast.picocli.command.scan_output.SCDastScanOutputCommands;
import com.fortify.cli.sc_dast.picocli.command.scan_settings.SCDastScanSettingsCommands;
import com.fortify.cli.sc_dast.picocli.command.sensor.SCDastSensorCommands;
import com.fortify.cli.ssc.picocli.command.session.SSCSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "sc-dast",
        description = "Commands for interacting with Fortify ScanCentral DAST.",
        subcommands = {
                SCDastScanCommands.class,
                SCDastScanOutputCommands.class,
                SCDastScanSettingsCommands.class,
                SCDastSensorCommands.class,
                SSCSessionCommands.class
        }
)
public class SCDastCommands {}
