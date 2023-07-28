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
package com.fortify.cli.license._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.license.msp_report.cli.cmd.MspReportCommands;
import com.fortify.cli.license.ncd_report.cli.cmd.NcdReportCommands;

import picocli.CommandLine.Command;

@Command(
        name = "license",
        resourceBundle = "com.fortify.cli.license.i18n.LicenseMessages",
        subcommands = {
            MspReportCommands.class,
            NcdReportCommands.class
        }
)
public class LicenseCommands extends AbstractFortifyCLICommand {}
