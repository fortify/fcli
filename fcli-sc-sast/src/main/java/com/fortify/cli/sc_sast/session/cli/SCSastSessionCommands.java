package com.fortify.cli.sc_sast.session.cli;

import picocli.CommandLine.Command;

@Command(
        name = "session",
        subcommands = {
        		SCSastSessionListCommand.class,
                SCSastSessionLoginCommand.class,
                SCSastSessionLogoutCommand.class 
        }
)
public class SCSastSessionCommands {
}
