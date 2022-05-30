package com.fortify.cli.fod.picocli.command;

import com.fortify.cli.fod.picocli.command.application.FoDApplicationCommands;
import com.fortify.cli.fod.picocli.command.application.release.FoDApplicationReleaseCommands;
import com.fortify.cli.fod.picocli.command.dast_scan.FoDDastScanCommands;
import com.fortify.cli.fod.picocli.command.sast_scan.FoDSastScanCommands;
import com.fortify.cli.fod.picocli.command.session.FoDSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "fod",
        description = "Commands for interacting with Fortify on Demand (FoD).",
        subcommands = {
                FoDApplicationCommands.class,
                FoDApplicationReleaseCommands.class,
                FoDDastScanCommands.class,
                FoDSastScanCommands.class,
                FoDSessionCommands.class
                
        }
)
public class FoDCommands {
}
