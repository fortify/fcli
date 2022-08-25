package com.fortify.cli.ssc.appversion_attribute.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-attribute",
        subcommands = {
            SSCAppVersionAttributeListCommand.class,
            SSCAppVersionAttributeUpdateCommand.class
        }
)
public class SSCAppVersionAttributeCommands {
}
