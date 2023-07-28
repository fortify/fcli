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

package com.fortify.cli.fod.scan.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.fod.scan.cli.cmd.dast.FoDDastScanImportCommand;
import com.fortify.cli.fod.scan.cli.cmd.dast.FoDDastScanStartCommand;
import com.fortify.cli.fod.scan.cli.cmd.mobile.FoDMobileScanImportCommand;
import com.fortify.cli.fod.scan.cli.cmd.mobile.FoDMobileScanStartCommand;
import com.fortify.cli.fod.scan.cli.cmd.oss.FoDOssScanImportCommand;
import com.fortify.cli.fod.scan.cli.cmd.sast.FoDSastScanImportCommand;
import com.fortify.cli.fod.scan.cli.cmd.sast.FoDSastScanSetupCommand;
import com.fortify.cli.fod.scan.cli.cmd.sast.FoDSastScanStartCommand;

import picocli.CommandLine;

@CommandLine.Command(name = "scan",
        subcommands = {
                FoDScanCancelCommand.class,
                FoDScanGetCommand.class,
                FoDScanListCommand.class,
                // commented out as single list command is probably sufficient
                //FoDSastScanListCommand.class,
                //FoDOssScanListCommand.class,
                //FoDDastScanListCommand.class,
                //FoDMobileScanListCommand.class,
                FoDSastScanImportCommand.class,
                FoDDastScanImportCommand.class,
                FoDOssScanImportCommand.class,
                FoDMobileScanImportCommand.class,
                FoDSastScanSetupCommand.class,
                FoDSastScanStartCommand.class,
                FoDDastScanStartCommand.class,
                FoDMobileScanStartCommand.class,
                FoDScanWaitForCommand.class
        }
)
@DefaultVariablePropertyName("scanId")
public class FoDScanCommands extends AbstractContainerCommand {
}
