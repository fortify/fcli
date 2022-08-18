package com.fortify.cli.ssc.picocli.command.attribute_definition;

import picocli.CommandLine.Command;

@Command(
        name = "attribute-definition",
        subcommands = {
        	SSCAttributeDefinitionListCommand.class
        }
)
public class SSCAttributeDefinitionCommands {
}
