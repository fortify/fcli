package com.fortify.cli.fod.entity.rest.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "rest",
        aliases = {},
        subcommands = {
                FoDRestCallCommand.class
        }

)
public class FoDRestCommands extends AbstractFortifyCLICommand {
}
