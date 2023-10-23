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

package com.fortify.cli.fod.sast_scan.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;

import picocli.CommandLine;

@CommandLine.Command(name = "sast-scan", aliases = "sast",
        subcommands = {
                FoDSastScanListCommand.class,
                FoDSastScanGetCommand.class,
                FoDSastScanGetConfigCommand.class,
                FoDSastScanSetupCommand.class,
                FoDSastScanStartCommand.class,
                FoDSastScanCancelCommand.class,
                FoDSastScanWaitForCommand.class,
                FoDSastScanImportCommand.class,
                FoDSastScanDownloadCommand.class,
                FoDSastScanDownloadLatestCommand.class
        }
)
@DefaultVariablePropertyName("scanId")
public class FoDSastScanCommands extends AbstractContainerCommand {
}
