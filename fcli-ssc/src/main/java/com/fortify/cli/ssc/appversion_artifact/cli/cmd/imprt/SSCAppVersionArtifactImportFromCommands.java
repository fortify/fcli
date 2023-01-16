package com.fortify.cli.ssc.appversion_artifact.cli.cmd.imprt;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.ssc.appversion_artifact.cli.cmd.imprt.debricked.SSCAppVersionArtifactImportFromDebrickedCommand;

import picocli.CommandLine.Command;

@Command(
        name = "import",
        subcommands = {
            SSCAppVersionArtifactImportFromDebrickedCommand.class
        }
)
public class SSCAppVersionArtifactImportFromCommands extends AbstractFortifyCLICommand {
}
