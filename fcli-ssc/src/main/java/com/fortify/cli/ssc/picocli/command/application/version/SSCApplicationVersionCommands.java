package com.fortify.cli.ssc.picocli.command.application.version;

import picocli.CommandLine.Command;

@Command(
        name = "application-version",
        aliases = {"av"},
        description = "Commands for interacting with application versions on Fortify SSC.",
        subcommands = {
        	SSCApplicationVersionListCommand.class
        }
)
public class SSCApplicationVersionCommands {
}
