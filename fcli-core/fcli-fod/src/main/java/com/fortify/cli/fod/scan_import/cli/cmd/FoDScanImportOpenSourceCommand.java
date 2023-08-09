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

import java.util.function.BiFunction;

import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.rest.FoDUrls;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.ImportOpenSource.CMD_NAME)
public class FoDScanImportOpenSourceCommand extends AbstractFoDScanImportCommand {
    @Getter @Mixin private FoDOutputHelperMixins.ImportOpenSource outputHelper;
    
    @Option(names="--type", required = true, defaultValue = "CycloneDX")
    private FoDScanImportOpenSourceType type; 
    
    @Override
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId) {
        return unirest.put(FoDUrls.MOBILE_SCANS_IMPORT).routeParam("relId", releaseId);
    }
    
    @RequiredArgsConstructor
    public static enum FoDScanImportOpenSourceType {
        CycloneDX(FoDScanImportOpenSourceType::getBaseRequestCycloneDX);
        
        private final BiFunction<UnirestInstance, String, HttpRequest<?>> f;
        
        public HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId) {
            return f.apply(unirest, releaseId);
        }
        
        private static final HttpRequest<?> getBaseRequestCycloneDX(UnirestInstance unirest, String releaseId) {
            return unirest.put(FoDUrls.RELEASE_IMPORT_CYCLONEDX_SBOM).routeParam("relId", releaseId);
        }
    }
}
