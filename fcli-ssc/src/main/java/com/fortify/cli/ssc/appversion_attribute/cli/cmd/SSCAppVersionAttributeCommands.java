package com.fortify.cli.ssc.appversion_attribute.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-attribute",
        subcommands = {
            SSCAppVersionAttributeListCommand.class,
            SSCAppVersionAttributeSetCommand.class
        }
)
public class SSCAppVersionAttributeCommands {
}
