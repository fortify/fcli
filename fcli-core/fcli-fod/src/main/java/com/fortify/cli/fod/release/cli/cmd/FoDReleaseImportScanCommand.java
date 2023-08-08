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

package com.fortify.cli.fod.release.cli.cmd;

import java.io.File;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDFileTransferHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.ImportScan.CMD_NAME)
public class FoDReleaseImportScanCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.ImportScan outputHelper;
    
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.PositionalParameter releaseResolver;

    @Option(names = {"-f", "--file"}, required = true)
    private File scanFile;
    
    @Option(names="--type", required = true)
    private FoDReleaseScanImportType type; 

    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
        var releaseId = releaseDescriptor.getReleaseId();
        var importScanSessionId = getImportScanSessionId(unirest, releaseId);
        HttpRequest<?> baseRequest = type.getBaseRequest(unirest, releaseId)
                .queryString("importScanSessionId", importScanSessionId)
                .queryString("fileLength", scanFile.length());
        FoDFileTransferHelper.uploadChunked(unirest, baseRequest, scanFile);
        return releaseDescriptor.asJsonNode();
    }

    public final JsonNode transformRecord(JsonNode record) {
        return FoDScanHelper.renameFields(record);
    }

    @Override
    public final String getActionCommandResult() {
        return "IMPORT_REQUESTED";
    }

    @Override
    public final boolean isSingular() {
        return true;
    }
    
    private static final String getImportScanSessionId(UnirestInstance unirest, String relId) {
        return unirest.get(FoDUrls.RELEASE_IMPORT_SCAN_SESSION)
                .routeParam("relId", relId)
                .asObject(ObjectNode.class)
                .getBody()
                .get("importScanSessionId")
                .asText();
    }
    
    @RequiredArgsConstructor
    public static enum FoDReleaseScanImportType {
        Dast(FoDReleaseScanImportType::getBaseRequestDast),
        Sast(FoDReleaseScanImportType::getBaseRequestSast),
        Mobile(FoDReleaseScanImportType::getBaseRequestMobile),
        CycloneDX(FoDReleaseScanImportType::getBaseRequestCycloneDX);
        
        private final BiFunction<UnirestInstance, String, HttpRequest<?>> f;
        
        public HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId) {
            return f.apply(unirest, releaseId);
        }
        
        private static final HttpRequest<?> getBaseRequestSast(UnirestInstance unirest, String releaseId) {
            return unirest.put(FoDUrls.STATIC_SCANS_IMPORT).routeParam("relId", releaseId);
        }
        
        private static final HttpRequest<?> getBaseRequestDast(UnirestInstance unirest, String releaseId) {
            return unirest.put(FoDUrls.DYNAMIC_SCANS_IMPORT).routeParam("relId", releaseId);
        }
        
        private static final HttpRequest<?> getBaseRequestMobile(UnirestInstance unirest, String releaseId) {
            return unirest.put(FoDUrls.MOBILE_SCANS_IMPORT).routeParam("relId", releaseId);
        }
        
        private static final HttpRequest<?> getBaseRequestCycloneDX(UnirestInstance unirest, String releaseId) {
            return unirest.put(FoDUrls.RELEASE_IMPORT_CYCLONEDX_SBOM).routeParam("relId", releaseId);
        }
    }
}
