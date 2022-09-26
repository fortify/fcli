package com.fortify.cli.ssc.appversion.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "appversion",
        subcommands = {
            SSCAppVersionCreateCommand.class,
            SSCAppVersionDeleteCommand.class,
            SSCAppVersionGetCommand.class,
            SSCAppVersionListCommand.class,
            SSCAppVersionUpdateCommand.class
        }
)
public class SSCAppVersionCommands {
}
