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

import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanFileUploadCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDDastFileTypeMixins;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = FoDOutputHelperMixins.UploadFile.CMD_NAME)
public class FoDDastScanFileUploadCommand extends AbstractFoDScanFileUploadCommand {
    @Getter @Mixin private FoDOutputHelperMixins.UploadFile outputHelper;

    @Mixin private FoDDastFileTypeMixins.RequiredOption dastFileType;

    @Override
    protected String getFileType() {
        return dastFileType.getDastFileType().name();
    }

    @Override
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId) {
        return unirest.patch(FoDUrls.DAST_AUTOMATED_SCANS + "/scan-setup/file-upload")
                .routeParam("relId", releaseId)
                .queryString("dastFileType", getFileType());
    }

}
