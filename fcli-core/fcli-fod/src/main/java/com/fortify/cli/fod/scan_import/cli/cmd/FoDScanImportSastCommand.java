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
package com.fortify.cli.fod.scan_import.cli.cmd;

import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.rest.FoDUrls;

import kong.unirest.core.HttpRequest;
import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = FoDOutputHelperMixins.ImportSast.CMD_NAME)
public class FoDScanImportSastCommand extends AbstractFoDScanImportCommand {
    @Getter @Mixin private FoDOutputHelperMixins.ImportSast outputHelper;
    
    @Override
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId) {
        return unirest.put(FoDUrls.STATIC_SCANS_IMPORT).routeParam("relId", releaseId);
    }
}
