package com.fortify.cli.ssc.rest.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "rest",
        aliases = {},
        subcommands = {
                SSCRestCallCommand.class
        }

)
public class SSCRestCommands {
}
