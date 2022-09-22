package com.fortify.cli.ssc.appversion.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "appversion",
        subcommands = {
            SSCAppVersionDeleteCommand.class,
            SSCAppVersionListCommand.class,
            SSCAppVersionGetCommand.class
        }
)
public class SSCAppVersionCommands {
}
