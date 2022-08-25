package com.fortify.cli.sc_sast.sensor.cli.cmd;

import com.fortify.cli.common.dummy.cli.cmd.DummyCommand;

import picocli.CommandLine.Command;

@Command(
        name = "sensor",
        aliases = {"worker"},
        subcommands = {
                DummyCommand.class
        }
)
public class SCSastSensorCommands {
}
