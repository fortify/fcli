package com.fortify.cli.ssc.role.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "role",
        aliases = {},
        subcommands = {
                SSCRoleListCommand.class
        }

)
public class SSCRoleCommands {
}
