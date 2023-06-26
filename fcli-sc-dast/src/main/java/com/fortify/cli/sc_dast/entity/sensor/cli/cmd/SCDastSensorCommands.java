package com.fortify.cli.sc_dast.entity.sensor.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;

import picocli.CommandLine.Command;

@Command(
        name = "sensor",
        subcommands = {
            SCDastSensorDisableCommand.class,
            SCDastSensorEnableCommand.class,
            SCDastSensorGetCommand.class,
            SCDastSensorListCommand.class
        }
)
@DefaultVariablePropertyName("id")
public class SCDastSensorCommands extends AbstractFortifyCLICommand {
}
