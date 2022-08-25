package com.fortify.cli.sc_sast.session.cli.cmd;

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
