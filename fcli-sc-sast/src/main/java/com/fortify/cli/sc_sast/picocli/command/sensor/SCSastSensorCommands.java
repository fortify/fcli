package com.fortify.cli.sc_sast.picocli.command.sensor;

import com.fortify.cli.common.picocli.command.DummyCommand;
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
