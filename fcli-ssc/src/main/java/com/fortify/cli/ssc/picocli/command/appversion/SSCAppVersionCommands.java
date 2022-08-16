package com.fortify.cli.ssc.picocli.command.appversion;

import picocli.CommandLine.Command;

@Command(
        name = "appversion",
        subcommands = {
        	SSCAppVersionListCommand.class,
            SSCAppVersionGetCommand.class
        }
)
public class SSCAppVersionCommands {
}
