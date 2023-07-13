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

package com.fortify.cli.fod.scan.helper.mobile;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDUploadResponse;
import com.fortify.cli.fod.release.helper.FoDAppRelDescriptor;
import com.fortify.cli.fod.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanTypeOptions;
import com.fortify.cli.fod.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDStartScan;

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
    public static final FoDScanDescriptor startScan(UnirestInstance unirest, IProgressWriterI18n progressWriter, String relId, FoDStartMobileScanRequest req,
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
            .put("scanType", FoDScanTypeOptions.FoDScanType.Mobile.name())
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
