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
package com.fortify.cli.sc_sast._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.sc_sast._common.session.cli.cmd.SCSastSessionCommands;
import com.fortify.cli.sc_sast.rest.cli.cmd.SCSastControllerRestCommands;
import com.fortify.cli.sc_sast.scan.cli.cmd.SCSastScanCommands;
import com.fortify.cli.sc_sast.sensor.cli.cmd.SCSastSensorCommands;

import picocli.CommandLine.Command;

@Command(
        name = "sc-sast",
        resourceBundle = "com.fortify.cli.sc_sast.i18n.SCSastMessages",
        subcommands = {
                // This list of product subcommands should be in alphabetical
                // order, except for:
                // - session command (should be the first command, as it is a 
                //   prerequisite for all other commands)
                // - rest command (should be the last command, as it's a low-level
                //   command and looks better in the usage command list, as usually 
                //   'rest' has a different header ('Interact with' compared to most 
                //   other commands ('Manage').
                SCSastSessionCommands.class,
                SCSastScanCommands.class,
                SCSastSensorCommands.class,
                SCSastControllerRestCommands.class,
        }
)
public class SCSastCommands extends AbstractContainerCommand {}
