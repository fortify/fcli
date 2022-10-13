package com.fortify.cli.ssc.appversion.cli.cmd;

import com.fortify.cli.common.variable.MinusVariableDefinition;

import picocli.CommandLine.Command;

@Command(
        name = "appversion",
        subcommands = {
            SSCAppVersionCreateCommand.class,
            SSCAppVersionDeleteCommand.class,
            SSCAppVersionGetCommand.class,
            SSCAppVersionListCommand.class,
            SSCAppVersionUpdateCommand.class
        }
)
@MinusVariableDefinition(name = "currentAppVersion", field = "id")
public class SSCAppVersionCommands {
}
