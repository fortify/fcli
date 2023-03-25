package com.fortify.cli.fod._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.fod.entity.app.cli.cmd.FoDAppCommands;
import com.fortify.cli.fod.entity.lookup.cli.cmd.FoDLookupCommands;
import com.fortify.cli.fod.entity.microservice.cli.cmd.FoDAppMicroserviceCommands;
import com.fortify.cli.fod.entity.release.cli.cmd.FoDAppRelCommands;
import com.fortify.cli.fod.entity.rest.cli.cmd.FoDRestCommands;
import com.fortify.cli.fod.entity.scan.cli.cmd.FoDScanCommands;
import com.fortify.cli.fod.entity.user.cli.cmd.FoDUserCommands;
import com.fortify.cli.fod.entity.user_group.cli.cmd.FoDUserGroupCommands;
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
                FoDAppRelCommands.class,
                FoDAppMicroserviceCommands.class,
                FoDLookupCommands.class,
                FoDScanCommands.class,
                FoDUserCommands.class,
                FoDUserGroupCommands.class
        }
)
public class FoDCommands extends AbstractFortifyCLICommand {}
