package com.fortify.cli.fod._main.cli.cmd;

import com.fortify.cli.fod.app.cli.cmd.FoDAppCommands;
import com.fortify.cli.fod.apprelease.cli.cmd.FoDReleaseCommands;
import com.fortify.cli.fod.dast_scan.cli.cmd.FoDDastScanCommands;
import com.fortify.cli.fod.sast_scan.cli.cmd.FoDSastScanCommands;
import com.fortify.cli.fod.session.cli.cmd.FoDSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "fod",
        resourceBundle = "com.fortify.cli.fod.i18n.FoDMessages",
        hidden = true,
        subcommands = {
                FoDAppCommands.class,
                FoDReleaseCommands.class,
                FoDDastScanCommands.class,
                FoDSastScanCommands.class,
                FoDSessionCommands.class
                
        }
)
public class FoDCommands {}
