/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.fod.sast_scan.cli.cmd;

import com.fortify.cli.fod._common.output.cli.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanImportCommand;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = FoDOutputHelperMixins.ImportSarif.CMD_NAME)
public class FoDSastScanImportSarifCommand extends AbstractFoDScanImportCommand {
    @Getter @Mixin private FoDOutputHelperMixins.ImportSarif outputHelper;

    @Override
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId) {
        return unirest.put(FoDUrls.STATIC_SCANS_IMPORT_SARIF).routeParam("relId", releaseId);
    }

    @Override
    protected FoDScanType getScanType() {
        return FoDScanType.Static;
    }
}
