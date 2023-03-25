package com.fortify.cli.ssc.entity.attribute_definition.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "attribute-definition",
        subcommands = {
            SSCAttributeDefinitionGetCommand.class,
            SSCAttributeDefinitionListCommand.class
        }
)
public class SSCAttributeDefinitionCommands extends AbstractFortifyCLICommand {
}
