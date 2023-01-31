package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.ssc.appversion_artifact.cli.cmd.imprt.SSCAppVersionArtifactImportFromCommands;
import com.fortify.cli.ssc.appversion_artifact.cli.cmd.purge.SSCAppVersionArtifactPurgeCommands;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-artifact",
        subcommands = {
            SSCAppVersionArtifactApproveCommand.class,
            SSCAppVersionArtifactDeleteCommand.class,
            SSCAppVersionArtifactDownloadCommand.class,
            SSCAppVersionArtifactGetCommand.class,
            SSCAppVersionArtifactImportFromCommands.class,
            SSCAppVersionArtifactListCommand.class,
            SSCAppVersionArtifactPurgeCommands.class,
            SSCAppVersionArtifactUploadCommand.class,
            SSCAppVersionArtifactWaitForCommand.class
        }
)
@DefaultVariablePropertyName("id")
public class SSCAppVersionArtifactCommands extends AbstractFortifyCLICommand {
}
