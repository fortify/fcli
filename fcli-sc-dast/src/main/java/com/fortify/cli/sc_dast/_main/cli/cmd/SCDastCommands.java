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
package com.fortify.cli.sc_dast._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.sc_dast.entity.rest.cli.cmd.SCDastRestCommands;
import com.fortify.cli.sc_dast.entity.scan.cli.cmd.SCDastScanCommands;
import com.fortify.cli.sc_dast.entity.scan_policy.cli.cmd.SCDastScanPolicyCommands;
import com.fortify.cli.sc_dast.entity.scan_settings.cli.cmd.SCDastScanSettingsCommands;
import com.fortify.cli.sc_dast.entity.sensor.cli.cmd.SCDastSensorCommands;
import com.fortify.cli.sc_dast.session.cli.cmd.SCDastSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "sc-dast",
        resourceBundle = "com.fortify.cli.sc_dast.i18n.SCDastMessages",
        subcommands = {
                // This list of product subcommands should be in alphabetical
                // order, except for:
                // - session command (should be the first command, as it is a 
                //   prerequisite for all other commands)
                // - rest command (should be the last command, as it's a low-level
                //   command and looks better in the usage command list, as usually 
                //   'rest' has a different header ('Interact with' compared to most 
                //   other commands ('Manage').
                SCDastSessionCommands.class,
                SCDastScanCommands.class,
                SCDastScanPolicyCommands.class,
                SCDastScanSettingsCommands.class,
                SCDastSensorCommands.class,
                SCDastRestCommands.class,
        }
)
public class SCDastCommands extends AbstractFortifyCLICommand {}
