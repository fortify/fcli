package com.fortify.cli.ssc.entity.appversion_filterset.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-filterset", aliases = "appversion-view",
        subcommands = {
            SSCAppVersionFilterSetGetCommand.class,
            SSCAppVersionFilterSetListCommand.class,
        }
)
public class SSCAppVersionFilterSetCommands extends AbstractFortifyCLICommand {
}
