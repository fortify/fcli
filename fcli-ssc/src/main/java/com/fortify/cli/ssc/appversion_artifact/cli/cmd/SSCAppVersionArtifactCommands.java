package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-artifact",
        subcommands = {
            SSCAppVersionArtifactApproveCommand.class,
            SSCAppVersionArtifactDeleteCommand.class,
            SSCAppVersionArtifactDownloadCommand.class,
            SSCAppVersionArtifactGetCommand.class,
            SSCAppVersionArtifactListCommand.class,
            SSCAppVersionArtifactPurgeCommand.class,
            SSCAppVersionArtifactUploadCommand.class
        }
)
public class SSCAppVersionArtifactCommands {
}
