/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/

package com.fortify.cli.fod.scan.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.fod.scan_dast.cli.cmd.FoDDastScanImportCommand;
import com.fortify.cli.fod.scan_dast.cli.cmd.FoDDastScanStartCommand;
import com.fortify.cli.fod.scan_mobile.cli.cmd.FoDMobileScanImportCommand;
import com.fortify.cli.fod.scan_mobile.cli.cmd.FoDMobileScanStartCommand;
import com.fortify.cli.fod.scan_oss.cli.cmd.FoDOssScanImportCommand;
import com.fortify.cli.fod.scan_sast.cli.cmd.FoDSastScanImportCommand;
import com.fortify.cli.fod.scan_sast.cli.cmd.FoDSastScanSetupCommand;
import com.fortify.cli.fod.scan_sast.cli.cmd.FoDSastScanStartCommand;

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
