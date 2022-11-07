package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import com.fortify.cli.common.variable.PredefinedVariable;
import com.fortify.cli.ssc.appversion_artifact.cli.cmd.purge.SSCAppVersionArtifactPurgeCommands;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-artifact",
        subcommands = {
            SSCAppVersionArtifactApproveCommand.class,
            SSCAppVersionArtifactDeleteCommand.class,
            SSCAppVersionArtifactDownloadCommand.class,
            SSCAppVersionArtifactGetCommand.class,
            SSCAppVersionArtifactListCommand.class,
            SSCAppVersionArtifactPurgeCommands.class,
            SSCAppVersionArtifactUploadCommand.class,
            SSCAppVersionArtifactWaitForCommand.class
        }
)
@PredefinedVariable(name = "currentArtifact", field = "id")
public class SSCAppVersionArtifactCommands {
}
