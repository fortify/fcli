package com.fortify.cli.ssc.appversion_attribute.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-attribute",
        subcommands = {
            SSCAppVersionAttributeListCommand.class,
            SSCAppVersionAttributeSetCommand.class
        }
)
public class SSCAppVersionAttributeCommands extends AbstractFortifyCLICommand {
}
