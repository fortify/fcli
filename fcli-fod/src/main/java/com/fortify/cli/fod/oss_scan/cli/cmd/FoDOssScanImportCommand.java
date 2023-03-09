/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/

package com.fortify.cli.fod.oss_scan.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.oss_scan.cli.mixin.FoDSbomFormatOptions;
import com.fortify.cli.fod.oss_scan.helper.FoDOssHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.release.cli.mixin.FoDAppMicroserviceRelResolverMixin;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.helper.FoDUploadResponse;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanFormatOptions;
import com.fortify.cli.fod.scan.helper.FoDImportScan;
import com.fortify.cli.fod.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.util.FoDConstants;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Import.CMD_NAME)
public class FoDOssScanImportCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Import outputHelper;

    @Mixin private FoDAppMicroserviceRelResolverMixin.PositionalParameter appMicroserviceRelResolver;
    @Mixin private FoDSbomFormatOptions.OptionalOption sbomFormat;

    @CommandLine.Option(names = {"--chunk-size"})
    private int chunkSize = FoDConstants.DEFAULT_CHUNK_SIZE;

    @CommandLine.Option(names = {"-f", "--file"}, required = true)
    private File scanFile;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        String relId = appMicroserviceRelResolver.getAppMicroserviceRelId(unirest);
        String importUrl = FoDUrls.RELEASE_IMPORT_CYCLONEDX_SBOM;
        if (sbomFormat != null && sbomFormat.getSbomFormat() != null) {
            if (sbomFormat.getSbomFormat().equals(FoDSbomFormatOptions.FoDSbomFormat.CycloneDX)) {
                importUrl = FoDUrls.RELEASE_IMPORT_CYCLONEDX_SBOM;
            } else {
                throw new RuntimeException("Unknown SBOM format specified");
            }
        }
        HttpRequest<?> request = unirest.put(importUrl).routeParam("relId", relId);
        FoDImportScan importScanHelper = new FoDImportScan(
                unirest, relId, request, scanFile
        );
        importScanHelper.setChunkSize(chunkSize);
        FoDUploadResponse response = importScanHelper.upload();
        if (response != null) {
            // get latest scan as we cannot use the referenceId from import anywhere
            FoDScanDescriptor descriptor = FoDScanHelper.getLatestScanDescriptor(unirest, relId,
                    FoDScanFormatOptions.FoDScanType.OpenSource, true);
            return descriptor.asObjectNode()
                    .put("releaseId", relId)
                    .put("scanMethod", "SBOMImport")
                    .put("importReferenceId", (response != null ? response.getReferenceId() : "N/A"));
        }
        return null;
    }

    public JsonNode transformRecord(JsonNode record) {
        return FoDOssHelper.renameFields(record);
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
