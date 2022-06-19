package com.fortify.cli.ssc.picocli.command.session;

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
