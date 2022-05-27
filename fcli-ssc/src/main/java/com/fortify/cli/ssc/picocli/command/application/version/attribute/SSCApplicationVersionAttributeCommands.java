package com.fortify.cli.ssc.picocli.command.application.version.attribute;

import picocli.CommandLine.Command;

@Command(
        name = "application-version-attribute",
        aliases = {"av-attribute"},
        description = "Commands for interacting with application version attributes on Fortify SSC.",
        subcommands = {
        	SSCApplicationVersionAttributeListCommand.class
        }
)
public class SSCApplicationVersionAttributeCommands {
}
