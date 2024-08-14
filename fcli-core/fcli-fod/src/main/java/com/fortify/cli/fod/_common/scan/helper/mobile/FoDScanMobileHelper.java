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

package com.fortify.cli.fod._common.scan.helper.mobile;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDFileTransferHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.FoDStartScanResponse;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDScanMobileHelper extends FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // TODO Split into multiple methods
    public static final FoDScanDescriptor startScan(UnirestInstance unirest, IProgressWriterI18n progressWriter, FoDReleaseDescriptor releaseDescriptor, FoDScanMobileStartRequest req,
                                                    File scanFile) {
        var relId = releaseDescriptor.getReleaseId();
        HttpRequest<?> request = unirest.post(FoDUrls.MOBILE_SCANS_START).routeParam("relId", relId)
                .queryString("startDate", (req.getStartDate()))
                .queryString("assessmentTypeId", req.getAssessmentTypeId())
                .queryString("frameworkType", req.getFrameworkType())
                .queryString("platformType", req.getPlatformType())
                .queryString("timeZone", req.getTimeZone())
                .queryString("entitlementFrequencyType", req.getEntitlementFrequencyType());

        if (req.getEntitlementId() != null && req.getEntitlementId() > 0) {
            request = request.queryString("entitlementId", req.getEntitlementId());
        }

        JsonNode response = FoDFileTransferHelper.uploadChunked(unirest, request, scanFile);
        FoDStartScanResponse startScanResponse = JsonHelper.treeToValue(response, FoDStartScanResponse.class);
        if (startScanResponse == null || startScanResponse.getScanId() <= 0) {
            throw new RuntimeException("Unable to retrieve scan id from response when starting Static scan.");
        }
        JsonNode node = objectMapper.createObjectNode()
            .put("scanId", startScanResponse.getScanId())
            .put("scanType", FoDScanType.Mobile.name())
            .put("releaseAndScanId",  String.format("%s:%s", releaseDescriptor.getReleaseId(), startScanResponse.getScanId()))
            .put("analysisStatusType", "Pending")
            .put("applicationName", releaseDescriptor.getApplicationName())
            .put("releaseName", releaseDescriptor.getReleaseName())
            .put("microserviceName", releaseDescriptor.getMicroserviceName());
        return JsonHelper.treeToValue(node, FoDScanDescriptor.class);
    }
}
