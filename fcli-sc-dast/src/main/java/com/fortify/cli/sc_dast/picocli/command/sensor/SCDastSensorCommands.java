package com.fortify.cli.sc_dast.picocli.command.sensor;

import com.fortify.cli.common.cli.DummyCommand;

import picocli.CommandLine.Command;

@Command(
        name = "sensor",
        description = "Commands for interacting with Fortify ScanCentral DAST sensors (workers).",
        aliases = {"worker"},
        subcommands = {
                DummyCommand.class
        }
)
public class SCDastSensorCommands {
}
