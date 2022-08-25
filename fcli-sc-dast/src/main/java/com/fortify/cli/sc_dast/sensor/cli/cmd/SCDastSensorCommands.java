package com.fortify.cli.sc_dast.sensor.cli.cmd;

import com.fortify.cli.common.dummy.cli.cmd.DummyCommand;

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
