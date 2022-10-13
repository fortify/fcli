package com.fortify.cli.ssc.app.cli.cmd;

import com.fortify.cli.common.variable.MinusVariableDefinition;

import picocli.CommandLine.Command;

@Command(
        name = "app",
        subcommands = {
                SSCAppDeleteCommand.class,
                SSCAppGetCommand.class,
                SSCAppListCommand.class,
                SSCAppUpdateCommand.class
        }
)
@MinusVariableDefinition(name = "currentApp", field = "id")
public class SSCAppCommands {
}
