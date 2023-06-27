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
                // This list of subcommands starts with generic session and rest commands,
                // followed by all entity commands in alphabetical order
                SCDastSessionCommands.class,
                SCDastRestCommands.class,
                SCDastScanCommands.class,
                SCDastScanPolicyCommands.class,
                SCDastScanSettingsCommands.class,
                SCDastSensorCommands.class,
        }
)
public class SCDastCommands extends AbstractFortifyCLICommand {}
