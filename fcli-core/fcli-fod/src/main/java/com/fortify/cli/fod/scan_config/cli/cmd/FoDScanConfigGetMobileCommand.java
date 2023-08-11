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
package com.fortify.cli.fod.scan_config.cli.cmd;

import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.scan_config.helper.FoDScanConfigMobileDescriptor;
import com.fortify.cli.fod.scan_config.helper.FoDScanConfigMobileHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = FoDOutputHelperMixins.GetMobile.CMD_NAME)
public class FoDScanConfigGetMobileCommand extends AbstractFoDScanConfigGetCommand {
    @Getter @Mixin private FoDOutputHelperMixins.GetMobile outputHelper;
    
    @Override
    protected FoDScanConfigMobileDescriptor getDescriptor(UnirestInstance unirest, String releaseId) {
        return FoDScanConfigMobileHelper.getSetupDescriptor(unirest, releaseId);
    }
}
