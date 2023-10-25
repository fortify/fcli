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
package com.fortify.cli.sc_dast.scan.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanCompleteCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanDeleteCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanImportFindingsCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanPauseCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanPublishCommand;
import com.fortify.cli.sc_dast.scan.cli.cmd.action.SCDastScanResumeCommand;

import picocli.CommandLine.Command;

@Command(
        name = "scan",
        subcommands = {
                SCDastScanCompleteCommand.class,
                SCDastScanDeleteCommand.class,
                SCDastScanDownloadCommand.class,
                SCDastScanGetCommand.class,
                SCDastScanImportFindingsCommand.class,
                SCDastScanListCommand.class,
                SCDastScanPauseCommand.class,
                SCDastScanPublishCommand.class,
                SCDastScanResumeCommand.class,
                SCDastScanStartCommand.class,
                SCDastScanWaitForCommand.class
        }
)
@DefaultVariablePropertyName("id")
public class SCDastScanCommands extends AbstractContainerCommand {
}
