package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

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
