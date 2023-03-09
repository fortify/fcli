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

package com.fortify.cli.fod.sast_scan.helper;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod.release.helper.FoDAppRelDescriptor;
import com.fortify.cli.fod.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.helper.FoDUploadResponse;
import com.fortify.cli.fod.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDScanNotFoundException;
import com.fortify.cli.fod.scan.helper.FoDStartScan;
import com.fortify.cli.fod.util.FoDConstants;
import com.fortify.cli.fod.util.FoDEnums;
import com.fortify.cli.fod.util.FoDQueryHelper;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDSastScanHelper extends FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final FoDSastScanSetupDescriptor setupScan(UnirestInstance unirest, Integer relId, FoDSetupSastScanRequest setupSastScanRequest) {
        ObjectNode body = objectMapper.valueToTree(setupSastScanRequest);
        FoDQueryHelper.stripNulls(body);
        unirest.put(FoDUrls.STATIC_SCANS + "/scan-setup")
                .routeParam("relId", String.valueOf(relId))
                .body(body).asObject(JsonNode.class).getBody();
        return getSetupDescriptor(unirest, String.valueOf(relId));
    }

    public static final FoDScanDescriptor startScan(UnirestInstance unirest, String relId, FoDStartSastScanRequest req,
                                                    File scanFile, int chunkSize) {
        FoDAppRelDescriptor appRelDescriptor = FoDAppRelHelper.getAppRelDescriptor(unirest, relId, ":", true);
        HttpRequest<?> request = unirest.post(FoDUrls.STATIC_SCAN_START).routeParam("relId", relId)
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
            String truncatedNotes = abbreviateString(req.getNotes(), FoDConstants.MAX_NOTES_LENGTH);
            request = request.queryString("notes", truncatedNotes);
        }
        FoDStartScan startScan = new FoDStartScan(unirest, relId, request, scanFile);
        startScan.setChunkSize(chunkSize);
        FoDUploadResponse startScanResponse = startScan.upload();
        if (startScanResponse == null || startScanResponse.getScanId() <= 0) {
            throw new RuntimeException("Unable to retrieve scan id from response when starting Static scan.");
        }
        JsonNode node = objectMapper.createObjectNode();
        ((ObjectNode) node).put("scanId", startScanResponse.getScanId());
        ((ObjectNode) node).put("analysisStatusType", "Pending");
        FoDScanDescriptor scanDescriptor = JsonHelper.treeToValue(node, FoDScanDescriptor.class);
        try {
            scanDescriptor = getScanDescriptor(unirest, String.valueOf(startScanResponse.getScanId()));
        } catch (FoDScanNotFoundException ex) {
            scanDescriptor.setStatus("Unavailable");
        }
        scanDescriptor.setMicroserviceName(appRelDescriptor.getMicroserviceName());
        return scanDescriptor;
    }

    public static final FoDSastScanSetupDescriptor getSetupDescriptor(UnirestInstance unirest, String relId) {
        GetRequest request = unirest.get(FoDUrls.STATIC_SCANS + "/scan-setup")
                .routeParam("relId", relId);
        JsonNode setup = request.asObject(ObjectNode.class).getBody();
        return JsonHelper.treeToValue(setup, FoDSastScanSetupDescriptor.class);
    }

    //

    private static String abbreviateString(String input, int maxLength) {
        if (input.length() <= maxLength)
            return input;
        else
            return input.substring(0, maxLength);
    }
}
