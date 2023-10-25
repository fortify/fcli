/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.fod._common.session.cli.cmd.FoDSessionCommands;
import com.fortify.cli.fod.access_control.cli.cmd.FoDAccessControlCommands;
import com.fortify.cli.fod.app.cli.cmd.FoDAppCommands;
import com.fortify.cli.fod.dast_scan.cli.cmd.FoDDastScanCommands;
import com.fortify.cli.fod.mast_scan.cli.cmd.FoDMastScanCommands;
import com.fortify.cli.fod.microservice.cli.cmd.FoDMicroserviceCommands;
import com.fortify.cli.fod.oss_scan.cli.cmd.FoDOssScanCommands;
import com.fortify.cli.fod.release.cli.cmd.FoDReleaseCommands;
import com.fortify.cli.fod.rest.cli.cmd.FoDRestCommands;
import com.fortify.cli.fod.sast_scan.cli.cmd.FoDSastScanCommands;

import picocli.CommandLine.Command;

@Command(
        name = "fod",
        resourceBundle = "com.fortify.cli.fod.i18n.FoDMessages",
         subcommands = {
                // This list of product subcommands should be in alphabetical
                // order, except for:
                // - session command (should be the first command, as it is a
                //   prerequisite for all other commands)
                // - rest command (should be the last command, as it's a low-level
                //   command and looks better in the usage command list, as usually
                //   'rest' has a different header ('Interact with' compared to most
                //   other commands ('Manage').
                // - If it makes sense to 'group' related entities, like app, microservice
                //   and release
                FoDSessionCommands.class,
                FoDAccessControlCommands.class,
                FoDAppCommands.class,
                FoDMicroserviceCommands.class,
                FoDReleaseCommands.class,
                FoDSastScanCommands.class,
                FoDDastScanCommands.class,
                FoDMastScanCommands.class,
                FoDOssScanCommands.class,
                FoDRestCommands.class,

        }
)
public class FoDCommands extends AbstractContainerCommand {}
