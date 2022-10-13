package com.fortify.cli.ssc.appversion_artifact.cli.cmd.purge;

import picocli.CommandLine.Command;

@Command(
        name = "purge",
        subcommands = {
            SSCAppVersionArtifactPurgeByIdCommand.class,
            SSCAppVersionArtifactPurgeByDateCommand.class,
        }
)
public class SSCAppVersionArtifactPurgeCommands {
}
