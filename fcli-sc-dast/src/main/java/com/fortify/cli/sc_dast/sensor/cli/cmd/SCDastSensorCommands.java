package com.fortify.cli.sc_dast.sensor.cli.cmd;

import com.fortify.cli.common.variable.PredefinedVariable;

import picocli.CommandLine.Command;

@Command(
        name = "sensor",
        description = "Commands for interacting with Fortify ScanCentral DAST sensors (scanners).",
        aliases = {"scanner"},
        subcommands = {
            SCDastSensorDisableCommand.class,
            SCDastSensorEnableCommand.class,
            SCDastSensorGetCommand.class,
            SCDastSensorListCommand.class
        }
)
@PredefinedVariable(name = "_scdast_currentSensor", field = "id")
public class SCDastSensorCommands {
}
