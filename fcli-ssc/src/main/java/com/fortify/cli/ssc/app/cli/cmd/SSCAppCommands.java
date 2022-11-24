package com.fortify.cli.ssc.app.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
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
@PredefinedVariable(name = "_ssc_currentApp", field = "id")
public class SSCAppCommands extends AbstractFortifyCLICommand {
}
