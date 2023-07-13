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
package com.fortify.cli.sc_dast.scan.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.UnirestInstance;

public class SCDastScanHelper {
    private SCDastScanHelper() {}
    
    public static enum ScanAction {
        PauseScan, ResumeScan, DeleteScan, ClearTrackedScan, RetryImportScanResults, CompleteScan, RetryImportScanFindings
    }
    
    public static final SCDastScanDescriptor getScanDescriptor(UnirestInstance unirest, String scanId) {
        return getDescriptor(
                unirest.get(String.format("/api/v2/scans/%s/scan-summary", scanId)).asObject(JsonNode.class).getBody().get("item"));
    }
    
    public static final SCDastScanDescriptor performScanAction(UnirestInstance unirest, SCDastScanDescriptor descriptor, ScanAction action) {
        ObjectNode data = new ObjectMapper().createObjectNode().put("scanActionType", action.name());
        unirest.post("/api/v2/scans/{scanId}/scan-action")
            .routeParam("scanId", descriptor.getId())
            .body(data)
            .asObject(JsonNode.class).getBody();
        descriptor = getScanDescriptor(unirest, descriptor.getId());
        descriptor.asObjectNode().put("action", action.name().toUpperCase()+"_REQUESTED");
        return descriptor;
    }

    private static final SCDastScanDescriptor getDescriptor(JsonNode scanNode) {
        return JsonHelper.treeToValue(scanNode, SCDastScanDescriptor.class);
    }
}
