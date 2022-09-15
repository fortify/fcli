package com.fortify.cli.ssc.token_definition.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "token-definition",
        aliases = {},
        subcommands = {
                SSCTokenDefinitionListCommand.class
        }

)
public class SSCTokenDefinitionCommands {
}
