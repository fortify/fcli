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
package com.fortify.cli.fod.dast_scan.cli.cmd;

import com.fortify.cli.fod._common.output.cli.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanConfigGetCommand;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastLegacyDescriptor;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastLegacyHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = FoDOutputHelperMixins.GetConfigLegacy.CMD_NAME, hidden = true)
public class FoDDastLegacyScanGetConfigCommand extends AbstractFoDScanConfigGetCommand {
    @Getter @Mixin private FoDOutputHelperMixins.GetConfigLegacy outputHelper;

    @Override
    protected FoDScanConfigDastLegacyDescriptor getDescriptor(UnirestInstance unirest, String releaseId) {
        return FoDScanConfigDastLegacyHelper.getSetupDescriptor(unirest, releaseId);
    }
}
