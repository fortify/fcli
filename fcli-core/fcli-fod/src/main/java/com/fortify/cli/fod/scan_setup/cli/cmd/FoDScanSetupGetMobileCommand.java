/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.fod.scan_setup.cli.cmd;

import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.scan_setup.helper.FoDScanMobileSetupDescriptor;
import com.fortify.cli.fod.scan_setup.helper.FoDScanMobileSetupHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = FoDOutputHelperMixins.GetMobile.CMD_NAME)
public class FoDScanSetupGetMobileCommand extends AbstractFoDScanSetupGetCommand {
    @Getter @Mixin private FoDOutputHelperMixins.GetMobile outputHelper;
    
    @Override
    protected FoDScanMobileSetupDescriptor getDescriptor(UnirestInstance unirest, String releaseId) {
        return FoDScanMobileSetupHelper.getSetupDescriptor(unirest, releaseId);
    }
}
