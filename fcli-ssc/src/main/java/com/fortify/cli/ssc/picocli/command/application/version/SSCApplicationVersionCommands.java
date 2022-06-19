package com.fortify.cli.ssc.picocli.command.application.version;

import picocli.CommandLine.Command;

@Command(
        name = "application-version",
        aliases = {"av"},
        subcommands = {
        	SSCApplicationVersionListCommand.class
        }
)
public class SSCApplicationVersionCommands {
}
