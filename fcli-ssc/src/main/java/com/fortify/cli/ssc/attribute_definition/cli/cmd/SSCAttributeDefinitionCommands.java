package com.fortify.cli.ssc.attribute_definition.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "attribute-definition",
        subcommands = {
            SSCAttributeDefinitionGetCommand.class,
            SSCAttributeDefinitionListCommand.class
        }
)
public class SSCAttributeDefinitionCommands {
}
