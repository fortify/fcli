package com.fortify.cli.ssc.appversion_filterset.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-filterset", aliases = "appversion-view",
        subcommands = {
            SSCAppVersionFilterSetGetCommand.class,
            SSCAppVersionFilterSetListCommand.class,
        }
)
public class SSCAppVersionFilterSetCommands {
}
