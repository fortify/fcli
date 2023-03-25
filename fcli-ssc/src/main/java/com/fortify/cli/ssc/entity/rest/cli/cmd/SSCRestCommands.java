package com.fortify.cli.ssc.entity.rest.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "rest",
        aliases = {},
        subcommands = {
                SSCRestCallCommand.class
        }

)
public class SSCRestCommands extends AbstractFortifyCLICommand {
}
