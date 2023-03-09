package com.fortify.cli.fod._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.fod.app.cli.cmd.FoDAppCommands;
import com.fortify.cli.fod.dast_scan.cli.cmd.FoDDastScanCommands;
import com.fortify.cli.fod.lookup.cli.cmd.FoDLookupCommands;
import com.fortify.cli.fod.microservice.cli.cmd.FoDAppMicroserviceCommands;
import com.fortify.cli.fod.oss_scan.cli.cmd.FoDOssScanCommands;
import com.fortify.cli.fod.release.cli.cmd.FoDAppRelCommands;
import com.fortify.cli.fod.rest.cli.cmd.FoDRestCommands;
import com.fortify.cli.fod.sast_scan.cli.cmd.FoDSastScanCommands;
import com.fortify.cli.fod.scan.cli.cmd.FoDScanCommands;
import com.fortify.cli.fod.session.cli.cmd.FoDSessionCommands;
import com.fortify.cli.fod.user.cli.cmd.FoDUserCommands;
import com.fortify.cli.fod.user_group.cli.cmd.FoDUserGroupCommands;

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
                FoDAppRelCommands.class,
                FoDAppMicroserviceCommands.class,
                FoDDastScanCommands.class,
                FoDLookupCommands.class,
                FoDOssScanCommands.class,
                FoDSastScanCommands.class,
                FoDScanCommands.class,
                FoDUserCommands.class,
                FoDUserGroupCommands.class
        }
)
public class FoDCommands extends AbstractFortifyCLICommand {}
