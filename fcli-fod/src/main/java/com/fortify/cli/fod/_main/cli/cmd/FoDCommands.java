package com.fortify.cli.fod._main.cli.cmd;

import com.fortify.cli.fod.app.cli.cmd.FoDAppCommands;
import com.fortify.cli.fod.apprelease.cli.cmd.FoDReleaseCommands;
import com.fortify.cli.fod.dast_scan.cli.cmd.FoDDastScanCommands;
import com.fortify.cli.fod.rest.cli.cmd.FoDRestCommands;
import com.fortify.cli.fod.sast_scan.cli.cmd.FoDSastScanCommands;
import com.fortify.cli.fod.session.cli.cmd.FoDSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "fod",
        resourceBundle = "com.fortify.cli.fod.i18n.FoDMessages",
        hidden = true,
        subcommands = {
                // This list of subcommands starts with generic session and rest commands,
                // followed by all entity commands in alphabetical order
                FoDSessionCommands.class,
                FoDRestCommands.class,
                FoDAppCommands.class,
                FoDReleaseCommands.class,
                FoDDastScanCommands.class,
                FoDSastScanCommands.class,
        }
)
public class FoDCommands {}
