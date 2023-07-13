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

package com.fortify.cli.fod.scan.cli.cmd.mobile;

import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.scan.cli.cmd.AbstractFoDScanListCommand;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanTypeOptions;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = FoDOutputHelperMixins.ListMobile.CMD_NAME)
public class FoDMobileScanListCommand extends AbstractFoDScanListCommand {
    @Getter @Mixin FoDOutputHelperMixins.ListMobile outputHelper;

    @Override
    public String getScanType() {
        return FoDScanTypeOptions.FoDScanType.Mobile.name();
    }
}
