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

package com.fortify.cli.fod.entity.scan.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.fod.entity.scan_dast.cli.cmd.FoDDastScanImportCommand;
import com.fortify.cli.fod.entity.scan_dast.cli.cmd.FoDDastScanStartCommand;
import com.fortify.cli.fod.entity.scan_mobile.cli.cmd.FoDMobileScanImportCommand;
import com.fortify.cli.fod.entity.scan_mobile.cli.cmd.FoDMobileScanStartCommand;
import com.fortify.cli.fod.entity.scan_oss.cli.cmd.FoDOssScanImportCommand;
import com.fortify.cli.fod.entity.scan_sast.cli.cmd.FoDSastScanImportCommand;
import com.fortify.cli.fod.entity.scan_sast.cli.cmd.FoDSastScanSetupCommand;
import com.fortify.cli.fod.entity.scan_sast.cli.cmd.FoDSastScanStartCommand;

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
public class FoDScanCommands extends AbstractFortifyCLICommand {
}
