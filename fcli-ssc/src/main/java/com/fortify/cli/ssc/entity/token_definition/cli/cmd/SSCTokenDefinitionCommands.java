package com.fortify.cli.ssc.entity.token_definition.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "token-definition",
        aliases = {},
        subcommands = {
                SSCTokenDefinitionListCommand.class
        }

)
public class SSCTokenDefinitionCommands extends AbstractFortifyCLICommand {
}
