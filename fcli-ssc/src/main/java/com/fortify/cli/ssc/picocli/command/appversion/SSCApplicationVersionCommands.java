package com.fortify.cli.ssc.picocli.command.appversion;

import picocli.CommandLine.Command;

@Command(
        name = "application-version",
        aliases = {"av"},
        subcommands = {
        	SSCApplicationVersionListCommand.class,
            SSCApplicationVersionGetCommand.class
        }
)
public class SSCApplicationVersionCommands {
}
