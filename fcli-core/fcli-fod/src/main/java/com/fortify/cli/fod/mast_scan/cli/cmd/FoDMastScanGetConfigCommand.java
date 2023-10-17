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
package com.fortify.cli.fod.mast_scan.cli.cmd;

import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.mast_scan.helper.FoDScanConfigMobileDescriptor;
import com.fortify.cli.fod.mast_scan.helper.FoDScanConfigMobileHelper;
import com.fortify.cli.fod.scan.cli.cmd.AbstractFoDScanConfigGetCommand;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = FoDOutputHelperMixins.GetConfig.CMD_NAME)
public class FoDMastScanGetConfigCommand extends AbstractFoDScanConfigGetCommand {
    @Getter @Mixin private FoDOutputHelperMixins.GetConfig outputHelper;

    @Override
    protected FoDScanConfigMobileDescriptor getDescriptor(UnirestInstance unirest, String releaseId) {
        return FoDScanConfigMobileHelper.getSetupDescriptor(unirest, releaseId);
    }
}
