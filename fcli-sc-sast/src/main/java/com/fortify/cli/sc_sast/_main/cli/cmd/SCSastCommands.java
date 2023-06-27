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
