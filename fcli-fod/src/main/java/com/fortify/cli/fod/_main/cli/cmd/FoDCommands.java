/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
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
