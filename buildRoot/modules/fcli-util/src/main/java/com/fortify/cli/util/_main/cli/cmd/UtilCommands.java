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
package com.fortify.cli.util._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.util.all_commands.cli.cmd.AllCommandsCommands;
import com.fortify.cli.util.autocomplete.cli.cmd.AutoCompleteCommands;
import com.fortify.cli.util.crypto.cli.cmd.CryptoCommands;
import com.fortify.cli.util.msp_report.cli.cmd.MspReportCommands;
import com.fortify.cli.util.ncd_report.cli.cmd.NcdReportCommands;

import picocli.CommandLine.Command;

@Command(
        name = "util",
        resourceBundle = "com.fortify.cli.util.i18n.UtilMessages",
        subcommands = {
            AllCommandsCommands.class,
            AutoCompleteCommands.class,
            CryptoCommands.class,
            MspReportCommands.class,
            NcdReportCommands.class
        }
)
public class UtilCommands extends AbstractFortifyCLICommand {}
