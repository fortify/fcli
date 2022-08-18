package com.fortify.cli.ssc.picocli.command.appversion_attribute;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-attribute",
        subcommands = {
        	SSCAppVersionAttributeListCommand.class,
        	SSCAppVersionAttributeUpdateCommand.class
        }
)
public class SSCAppVersionAttributeCommands {
}
