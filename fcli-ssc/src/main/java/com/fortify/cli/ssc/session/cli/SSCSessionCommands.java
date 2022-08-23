package com.fortify.cli.ssc.session.cli;

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
