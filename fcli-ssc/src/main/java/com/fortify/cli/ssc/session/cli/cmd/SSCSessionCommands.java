package com.fortify.cli.ssc.session.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "session",
        subcommands = {
        		SSCSessionListCommand.class,
                SSCSessionLoginCommand.class,
                SSCSessionLogoutCommand.class 
        }
)
public class SSCSessionCommands {
}
