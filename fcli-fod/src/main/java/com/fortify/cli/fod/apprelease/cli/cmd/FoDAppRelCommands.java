package com.fortify.cli.fod.apprelease.cli.cmd;

import com.fortify.cli.common.variable.PredefinedVariable;
import picocli.CommandLine;

@CommandLine.Command(name = "release",
        aliases = {"application-release", "app-rel"},
        subcommands = {
                FoDAppRelCreateCommand.class,
                FoDAppRelListCommand.class,
                FoDAppRelGetCommand.class,
                FoDAppRelUpdateCommand.class,
                FoDAppRelDeleteCommand.class
        }
)
@PredefinedVariable(name = "currentRel", field = "id")
public class FoDAppRelCommands {
}
