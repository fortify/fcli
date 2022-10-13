package com.fortify.cli.sc_dast.rest.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "rest",
        aliases = {},
        subcommands = {
                SCDastRestCallCommand.class
        }

)
public class SCDastRestCommands {
}
