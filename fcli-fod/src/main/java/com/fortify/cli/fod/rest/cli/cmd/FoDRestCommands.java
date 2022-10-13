package com.fortify.cli.fod.rest.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "rest",
        aliases = {},
        subcommands = {
                FoDRestCallCommand.class
        }

)
public class FoDRestCommands {
}
