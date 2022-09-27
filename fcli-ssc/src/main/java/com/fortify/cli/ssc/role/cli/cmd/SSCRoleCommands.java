package com.fortify.cli.ssc.role.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "role",
        aliases = {},
        subcommands = {
                SSCRoleCreateCommand.class,
                SSCRoleDeleteCommand.class,
                SSCRoleGetCommand.class,
                SSCRoleListCommand.class
        }

)
public class SSCRoleCommands {
}
