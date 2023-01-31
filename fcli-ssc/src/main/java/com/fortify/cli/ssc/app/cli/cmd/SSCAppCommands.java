package com.fortify.cli.ssc.app.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;

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
@DefaultVariablePropertyName("id")
public class SSCAppCommands extends AbstractFortifyCLICommand {
}
