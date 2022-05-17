package com.fortify.cli.sc_dast.picocli.command.entity.sensor;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine.Command;

@Command(
        name = "sensor",
        description = "Commands for interacting with Fortify ScanCentral DAST sensors (workers).",
        aliases = {"worker"},
        subcommands = {
                DummyCommand.class
        }
)
public class SCDASTSensorCommands {
}
