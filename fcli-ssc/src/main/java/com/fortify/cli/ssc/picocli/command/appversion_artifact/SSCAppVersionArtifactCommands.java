package com.fortify.cli.ssc.picocli.command.appversion_artifact;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-artifact",
        subcommands = {
        	SSCAppVersionArtifactListCommand.class,
            SSCAppVersionArtifactDownloadCommand.class
        }
)
public class SSCAppVersionArtifactCommands {
}
