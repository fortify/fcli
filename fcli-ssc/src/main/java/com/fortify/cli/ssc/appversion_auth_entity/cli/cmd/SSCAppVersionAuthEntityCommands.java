package com.fortify.cli.ssc.appversion_auth_entity.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-user",
        subcommands = {
                SSCAppVersionAuthEntityAddCommand.class,
                SSCAppVersionAuthEntityDeleteCommand.class,
                SSCAppVersionAuthEntityListCommand.class,
        }
)
public class SSCAppVersionAuthEntityCommands {
}
