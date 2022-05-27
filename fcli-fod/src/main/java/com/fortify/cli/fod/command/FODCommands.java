package com.fortify.cli.fod.command;

import com.fortify.cli.fod.command.entity.application.FODApplicationCommands;
import com.fortify.cli.fod.command.entity.dast_scan.FODDASTScanCommands;
import com.fortify.cli.fod.command.entity.sast_scan.FODSASTScanCommands;

import picocli.CommandLine.Command;

@Command(
        name = "fod",
        description = "Commands for interacting with Fortify on Demand (FoD).",
        subcommands = {
                FODApplicationCommands.class,
                FODSASTScanCommands.class,
                FODDASTScanCommands.class
        }
)
public class FODCommands {
}
