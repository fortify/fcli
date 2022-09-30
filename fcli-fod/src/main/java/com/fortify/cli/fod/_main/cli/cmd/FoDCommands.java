package com.fortify.cli.fod._main.cli.cmd;

import com.fortify.cli.fod.app.cli.cmd.FoDApplicationCommands;
import com.fortify.cli.fod.apprelease.cli.cmd.FoDApplicationReleaseCommands;
import com.fortify.cli.fod.dast_scan.cli.cmd.FoDDastScanCommands;
import com.fortify.cli.fod.sast_scan.cli.cmd.FoDSastScanCommands;
import com.fortify.cli.fod.session.cli.cmd.FoDSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "fod",
        resourceBundle = "com.fortify.cli.fod.i18n.FoDMessages",
        hidden = true,
        subcommands = {
                FoDApplicationCommands.class,
                FoDApplicationReleaseCommands.class,
                FoDDastScanCommands.class,
                FoDSastScanCommands.class,
                FoDSessionCommands.class
                
        }
)
public class FoDCommands {}
