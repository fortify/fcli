package com.fortify.cli.sc_sast.picocli.command.entity.sensor;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine.Command;

@Command(
        name = "sensor",
        description = "Commands for working with Fortify ScanCental SAST sensors (workers).",
        aliases = {"worker"},
        subcommands = {
                DummyCommand.class
        }
)
public class SCSASTSensorCommands {
}
