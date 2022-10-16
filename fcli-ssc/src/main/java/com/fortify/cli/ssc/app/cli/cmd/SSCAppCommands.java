package com.fortify.cli.ssc.app.cli.cmd;

import com.fortify.cli.common.variable.PredefinedVariable;

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
@PredefinedVariable(name = "currentApp", field = "id")
public class SSCAppCommands {
}
