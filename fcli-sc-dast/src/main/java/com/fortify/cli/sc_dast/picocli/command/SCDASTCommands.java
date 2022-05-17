package com.fortify.cli.sc_dast.picocli.command;

import com.fortify.cli.sc_dast.picocli.command.entity.scan.SCDASTScanCommands;
import com.fortify.cli.sc_dast.picocli.command.entity.sensor.SCDASTSensorCommands;
import picocli.CommandLine.Command;

@Command(
        name = "sc-dast",
        description = "Commands for interacting with Fortify ScanCentral DAST.",
        subcommands = {
                SCDASTScanCommands.class,
                SCDASTSensorCommands.class
        }
)
public class SCDASTCommands {
}
