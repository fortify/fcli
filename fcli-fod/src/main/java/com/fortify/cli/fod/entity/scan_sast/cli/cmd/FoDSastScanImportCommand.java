/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/

package com.fortify.cli.fod.entity.scan_sast.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod.entity.release.cli.mixin.FoDAppMicroserviceRelResolverMixin;
import com.fortify.cli.fod.entity.scan.cli.mixin.FoDScanTypeOptions;
import com.fortify.cli.fod.entity.scan.helper.FoDImportScan;
import com.fortify.cli.fod.entity.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.entity.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.helper.FoDUploadResponse;
import com.fortify.cli.fod.util.FoDConstants;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = FoDOutputHelperMixins.ImportSast.CMD_NAME)
public class FoDSastScanImportCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.ImportSast outputHelper;

    @Mixin private FoDAppMicroserviceRelResolverMixin.PositionalParameter appMicroserviceRelResolver;

    @CommandLine.Option(names = {"--chunk-size"})
    private int chunkSize = FoDConstants.DEFAULT_CHUNK_SIZE;
    @CommandLine.Option(names = {"-f", "--file"}, required = true)
    private File scanFile;

    // TODO Split method in multiple methods for upload and generating output
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        String relId = appMicroserviceRelResolver.getAppMicroserviceRelId(unirest);
        HttpRequest<?> request = unirest.put(FoDUrls.STATIC_SCANS_IMPORT).routeParam("relId", relId);
        FoDImportScan importScanHelper = new FoDImportScan(
                unirest, relId, request, scanFile
        );
        importScanHelper.setChunkSize(chunkSize);
        FoDUploadResponse response = importScanHelper.upload();
        if (response != null) {
            // get latest scan as we cannot use the referenceId from import anywhere
            FoDScanDescriptor descriptor = FoDScanHelper.getLatestScanDescriptor(unirest, relId,
                    FoDScanTypeOptions.FoDScanType.Static, true);
            return descriptor.asObjectNode()
                    .put("releaseId", relId)
                    .put("scanMethod", "FPRImport")
                    .put("importReferenceId", (response != null ? response.getReferenceId() : "N/A"));
        }
        return null;
    }

    public JsonNode transformRecord(JsonNode record) {
        return FoDScanHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "IMPORTED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
