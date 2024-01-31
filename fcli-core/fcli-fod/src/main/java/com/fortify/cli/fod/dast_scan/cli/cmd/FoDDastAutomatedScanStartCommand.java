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

package com.fortify.cli.fod.dast_scan.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanStartCommand;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedHelper;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastAutomatedDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Start.CMD_NAME)
public class FoDDastAutomatedScanStartCommand extends AbstractFoDScanStartCommand {
    @Getter @Mixin private OutputHelperMixins.Start outputHelper;

    @Override
    protected FoDScanDescriptor startScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
            String relId = releaseDescriptor.getReleaseId();

            // get current setup to ensure the scan has been configured
            FoDScanConfigDastAutomatedDescriptor currentSetup = FoDScanDastAutomatedHelper.getSetupDescriptor(unirest, relId);

            return FoDScanDastAutomatedHelper.startScan(unirest, releaseDescriptor);
    }
}
