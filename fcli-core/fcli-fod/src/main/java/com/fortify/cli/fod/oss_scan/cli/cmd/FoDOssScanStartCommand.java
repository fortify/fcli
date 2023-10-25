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

package com.fortify.cli.fod.oss_scan.cli.cmd;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanStartCommand;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.oss.FoDScanOssHelper;
import com.fortify.cli.fod._common.scan.helper.oss.FoDScanOssStartRequest;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Start.CMD_NAME, hidden = false)
public class FoDOssScanStartCommand extends AbstractFoDScanStartCommand {
    @Getter @Mixin private OutputHelperMixins.Start outputHelper;
    @Mixin private CommonOptionMixins.RequiredFile scanFileMixin;
    
    @Override
    protected FoDScanDescriptor startScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        FoDScanOssStartRequest startScanRequest = FoDScanOssStartRequest.builder().build();
        return FoDScanOssHelper.startScanWithDefaults(unirest, releaseDescriptor, startScanRequest, scanFileMixin.getFile());
    }
}
