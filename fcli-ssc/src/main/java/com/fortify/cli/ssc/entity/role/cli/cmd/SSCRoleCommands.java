package com.fortify.cli.ssc.entity.role.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

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
public class SSCRoleCommands extends AbstractFortifyCLICommand {
}
