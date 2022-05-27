package com.fortify.cli.ssc.picocli.command;

import com.fortify.cli.ssc.picocli.command.api.SSCApiCommand;
import com.fortify.cli.ssc.picocli.command.application.SSCApplicationCommands;
import com.fortify.cli.ssc.picocli.command.application.version.SSCApplicationVersionCommands;
import com.fortify.cli.ssc.picocli.command.application.version.artifact.SSCApplicationVersionArtifactCommands;
import com.fortify.cli.ssc.picocli.command.application.version.attribute.SSCApplicationVersionAttributeCommands;
import com.fortify.cli.ssc.picocli.command.session.SSCSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "ssc",
        description = "Commands for interacting with Fortify Software Security Center (SSC).",
        subcommands = {
                SSCSessionCommands.class,
                SSCApiCommand.class,
                SSCApplicationCommands.class,
                SSCApplicationVersionCommands.class,
                SSCApplicationVersionArtifactCommands.class,
                SSCApplicationVersionAttributeCommands.class
        }
)
public class SSCCommands {
}
