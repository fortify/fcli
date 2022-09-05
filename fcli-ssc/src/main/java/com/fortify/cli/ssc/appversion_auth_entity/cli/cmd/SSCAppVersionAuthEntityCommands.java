package com.fortify.cli.ssc.appversion_auth_entity.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-user",
        subcommands = {
                SSCAppVersionAuthEntityListCommand.class,
                SSCAppVersionAuthEntityUpdateCommand.class
        }
)
public class SSCAppVersionAuthEntityCommands {
}
