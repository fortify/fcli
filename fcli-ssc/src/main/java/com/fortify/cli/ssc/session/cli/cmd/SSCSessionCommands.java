package com.fortify.cli.ssc.session.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "session",
        subcommands = {
                SSCSessionListCommand.class,
                SSCSessionLoginCommand.class,
                SSCSessionLogoutCommand.class 
        }
)
public class SSCSessionCommands extends AbstractFortifyCLICommand {
}
