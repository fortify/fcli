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

package com.fortify.cli.fod.entity.scan_mobile.helper;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressHelperI18n;
import com.fortify.cli.fod.entity.release.helper.FoDAppRelDescriptor;
import com.fortify.cli.fod.entity.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.entity.scan.cli.mixin.FoDScanFormatOptions;
import com.fortify.cli.fod.entity.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.entity.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.entity.scan.helper.FoDStartScan;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.helper.FoDUploadResponse;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDMobileScanHelper extends FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /*public static final FoDMobileScanSetupDescriptor setupScan(UnirestInstance unirest, Integer relId, FoDSetupMobileScanRequest setupMobileScanRequest) {
        ObjectNode body = objectMapper.valueToTree(setupMobileScanRequest);
        FoDQueryHelper.stripNulls(body);
        unirest.put(FoDUrls.STATIC_SCANS + "/scan-setup")
                .routeParam("relId", String.valueOf(relId))
                .body(body).asObject(JsonNode.class).getBody();
        return getSetupDescriptor(unirest, String.valueOf(relId));
    }*/

    // TODO Split into multiple methods
    public static final FoDScanDescriptor startScan(UnirestInstance unirest, IProgressHelperI18n progressHelper, String relId, FoDStartMobileScanRequest req,
                                                    File scanFile, int chunkSize) {
        FoDAppRelDescriptor appRelDescriptor = FoDAppRelHelper.getAppRelDescriptor(unirest, relId, ":", true);
        HttpRequest<?> request = unirest.post(FoDUrls.MOBILE_SCANS_START).routeParam("relId", relId)
                .queryString("startDate", (req.getStartDate()))
                .queryString("assessmentTypeId", req.getAssessmentTypeId())
                .queryString("frameworkType", req.getFrameworkType())
                .queryString("timeZone", req.getTimeZone())
                .queryString("entitlementFrequencyType", req.getEntitlementFrequencyType());

        if (req.getEntitlementId() != null && req.getEntitlementId() > 0) {
            request = request.queryString("entitlementId", req.getEntitlementId());
        }

        FoDStartScan startScan = new FoDStartScan(unirest, relId, request, scanFile);
        startScan.setChunkSize(chunkSize);
        FoDUploadResponse startScanResponse = startScan.upload();
        if (startScanResponse == null || startScanResponse.getScanId() <= 0) {
            throw new RuntimeException("Unable to retrieve scan id from response when starting Static scan.");
        }
        JsonNode node = objectMapper.createObjectNode()
            .put("scanId", startScanResponse.getScanId())
            .put("scanType", FoDScanFormatOptions.FoDScanType.Mobile.name())
            .put("analysisStatusType", "Pending")
            .put("applicationName", appRelDescriptor.getApplicationName())
            .put("releaseName", appRelDescriptor.getReleaseName())
            .put("microserviceName", appRelDescriptor.getMicroserviceName());
        return JsonHelper.treeToValue(node, FoDScanDescriptor.class);
    }

    public static final FoDMobileScanSetupDescriptor getSetupDescriptor(UnirestInstance unirest, String relId) {
        GetRequest request = unirest.get(FoDUrls.MOBILE_SCANS + "/scan-setup")
                .routeParam("relId", relId);
        JsonNode setup = request.asObject(ObjectNode.class).getBody();
        return JsonHelper.treeToValue(setup, FoDMobileScanSetupDescriptor.class);
    }

    // TODO Consider having a generic abbreviate method in StringUtils
    // TODO Consider adding commons-lang as fcli dependency, which already provides abbreviate method
    private static String abbreviateString(String input, int maxLength) {
        if (input.length() <= maxLength)
            return input;
        else
            return input.substring(0, maxLength);
    }
}
