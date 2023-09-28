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

package com.fortify.cli.fod.scan.helper.sast;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDFileTransferHelper;
import com.fortify.cli.fod._common.util.FoDConstants;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDScanType;
import com.fortify.cli.fod.scan.helper.FoDStartScanResponse;
import com.fortify.cli.fod.sast_scan.helper.FoDScanConfigSastDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDScanSastHelper extends FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final FoDScanDescriptor startScanWithDefaults(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                                                                FoDScanSastStartRequest req, File scanFile) {
        var relId = releaseDescriptor.getReleaseId();
        HttpRequest<?> request = unirest.post(FoDUrls.STATIC_SCAN_START_WITH_DEFAULTS).routeParam("relId", relId)
                .queryString("isRemediationScan", req.getIsRemediationScan())
                .queryString("scanTool", req.getScanTool())
                .queryString("scanToolVersion", req.getScanToolVersion())
                .queryString("scanMethodType", req.getScanMethodType());
        if (req.getNotes() != null && !req.getNotes().isEmpty()) {
            String truncatedNotes = StringUtils.abbreviate(req.getNotes(), FoDConstants.MAX_NOTES_LENGTH);
            request = request.queryString("notes", truncatedNotes);
        }
        return startScan(unirest, releaseDescriptor, request, scanFile);
    }

    public static final FoDScanDescriptor startScanAdvanced(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanSastStartRequest req,
                                                            File scanFile) {
        var relId = releaseDescriptor.getReleaseId();
        HttpRequest<?> request = unirest.post(FoDUrls.STATIC_SCAN_START_ADVANCED).routeParam("relId", relId)
                .queryString("entitlementPreferenceType", (req.getEntitlementPreferenceType() != null ?
                        FoDEnums.EntitlementPreferenceType.valueOf(req.getEntitlementPreferenceType()) : FoDEnums.EntitlementPreferenceType.SubscriptionFirstThenSingleScan))
                .queryString("purchaseEntitlement", Boolean.toString(req.getPurchaseEntitlement()))
                .queryString("remdiationScanPreferenceType", (req.getRemdiationScanPreferenceType() != null ?
                        FoDEnums.RemediationScanPreferenceType.valueOf(req.getRemdiationScanPreferenceType()) : FoDEnums.RemediationScanPreferenceType.NonRemediationScanOnly))
                .queryString("inProgressScanActionType", (req.getInProgressScanActionType() != null ?
                        FoDEnums.InProgressScanActionType.valueOf(req.getInProgressScanActionType()) : FoDEnums.InProgressScanActionType.DoNotStartScan))
                .queryString("scanTool", req.getScanTool())
                .queryString("scanToolVersion", req.getScanToolVersion())
                .queryString("scanMethodType", req.getScanMethodType());
        if (req.getEntitlementId() != null && req.getEntitlementId() > 0) {
            request = request.queryString("entitlementId", req.getEntitlementId());
        }
        if (req.getNotes() != null && !req.getNotes().isEmpty()) {
            String truncatedNotes = StringUtils.abbreviate(req.getNotes(), FoDConstants.MAX_NOTES_LENGTH);
            request = request.queryString("notes", truncatedNotes);
        }
        return startScan(unirest, releaseDescriptor, request, scanFile);
    }

    private static FoDScanDescriptor startScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, HttpRequest<?> request, File scanFile) {
        JsonNode response = FoDFileTransferHelper.uploadChunked(unirest, request, scanFile);
        FoDStartScanResponse startScanResponse = JsonHelper.treeToValue(response, FoDStartScanResponse.class);
        if (startScanResponse == null || startScanResponse.getScanId() <= 0) {
            throw new RuntimeException("Unable to retrieve scan id from response when starting Static scan.");
        }
        JsonNode node = objectMapper.createObjectNode()
                .put("scanId", startScanResponse.getScanId())
                .put("scanType", FoDScanType.Static.name())
                .put("analysisStatusType", "Pending")
                .put("applicationName", releaseDescriptor.getApplicationName())
                .put("releaseName", releaseDescriptor.getReleaseName())
                .put("microserviceName", releaseDescriptor.getMicroserviceName());
        return JsonHelper.treeToValue(node, FoDScanDescriptor.class);
    }

    public static final FoDScanConfigSastDescriptor getSetupDescriptor(UnirestInstance unirest, String relId) {
        GetRequest request = unirest.get(FoDUrls.STATIC_SCANS + "/scan-setup")
                .routeParam("relId", relId);
        JsonNode setup = request.asObject(ObjectNode.class).getBody()
                .put("applicationName", "test");
        return JsonHelper.treeToValue(setup, FoDScanConfigSastDescriptor.class);
    }

    public static final FoDScanConfigSastDescriptor getSetupDescriptorWithAppRel(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        GetRequest request = unirest.get(FoDUrls.STATIC_SCANS + "/scan-setup")
                .routeParam("relId", releaseDescriptor.getReleaseId());
        JsonNode setup = request.asObject(ObjectNode.class).getBody()
                .put("applicationName", releaseDescriptor.getApplicationName())
                .put("releaseName", releaseDescriptor.getReleaseName())
                .put("microserviceName", releaseDescriptor.getMicroserviceName());
        return JsonHelper.treeToValue(setup, FoDScanConfigSastDescriptor.class);
    }

}
