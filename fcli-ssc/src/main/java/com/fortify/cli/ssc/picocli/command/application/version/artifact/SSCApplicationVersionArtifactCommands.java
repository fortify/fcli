package com.fortify.cli.ssc.picocli.command.application.version.artifact;

import picocli.CommandLine.Command;

@Command(
        name = "application-version-artifact",
        aliases = {"av-artifact"},
        subcommands = {
        	SSCApplicationVersionArtifactListCommand.class
        }
)
public class SSCApplicationVersionArtifactCommands {
}
