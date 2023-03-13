package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.ssc.appversion_artifact.cli.cmd.import_debricked.SSCAppVersionArtifactImportDebrickedCommand;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-artifact",
        subcommands = {
            SSCAppVersionArtifactApproveCommand.class,
            SSCAppVersionArtifactDeleteCommand.class,
            SSCAppVersionArtifactDownloadByIdCommand.class,
            SSCAppVersionArtifactDownloadStateCommand.class,
            SSCAppVersionArtifactGetCommand.class,
            SSCAppVersionArtifactImportDebrickedCommand.class,
            SSCAppVersionArtifactListCommand.class,
            SSCAppVersionArtifactPurgeByIdCommand.class,
            SSCAppVersionArtifactPurgeOlderThanCommand.class,
            SSCAppVersionArtifactUploadCommand.class,
            SSCAppVersionArtifactWaitForCommand.class
        }
)
@DefaultVariablePropertyName("id")
public class SSCAppVersionArtifactCommands extends AbstractFortifyCLICommand {
}
