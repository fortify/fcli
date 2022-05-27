package com.fortify.cli.ssc.picocli.command;

import com.fortify.cli.ssc.picocli.command.entity.application.SSCApplicationCommands;
import com.fortify.cli.ssc.picocli.command.entity.session.SSCSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "ssc",
        description = "Commands for interacting with Fortify Software Security Center (SSC).",
        subcommands = {
                SSCSessionCommands.class,
                SSCApplicationCommands.class
        }
)
public class SSCCommands {
}
