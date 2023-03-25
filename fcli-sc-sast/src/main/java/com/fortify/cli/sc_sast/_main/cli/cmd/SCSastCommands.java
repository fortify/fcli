package com.fortify.cli.sc_sast._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.sc_sast.entity.rest.cli.cmd.SCSastControllerRestCommands;
import com.fortify.cli.sc_sast.entity.scan.cli.cmd.SCSastScanCommands;
import com.fortify.cli.sc_sast.session.cli.cmd.SCSastSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "sc-sast",
        resourceBundle = "com.fortify.cli.sc_sast.i18n.SCSastMessages",
        subcommands = {
                // This list of subcommands starts with generic session and rest commands,
                // followed by all entity commands in alphabetical order
                SCSastSessionCommands.class,
                SCSastControllerRestCommands.class,
                SCSastScanCommands.class
        }
)
public class SCSastCommands extends AbstractFortifyCLICommand {}
