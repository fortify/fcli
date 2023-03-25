package com.fortify.cli.ssc.entity.appversion_user.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-user",
        subcommands = {
                SSCAppVersionAuthEntityAddCommand.class,
                SSCAppVersionAuthEntityDeleteCommand.class,
                SSCAppVersionAuthEntityListCommand.class,
        }
)
public class SSCAppVersionAuthEntityCommands extends AbstractFortifyCLICommand {
}
