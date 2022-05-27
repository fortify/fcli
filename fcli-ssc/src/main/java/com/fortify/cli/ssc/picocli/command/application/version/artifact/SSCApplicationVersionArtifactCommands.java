package com.fortify.cli.ssc.picocli.command.application.version.artifact;

import picocli.CommandLine.Command;

@Command(
        name = "application-version-artifact",
        aliases = {"av-artifact"},
        description = "Commands for interacting with application version artifacts on Fortify SSC.",
        subcommands = {
        	SSCApplicationVersionArtifactListCommand.class
        }
)
public class SSCApplicationVersionArtifactCommands {
}
