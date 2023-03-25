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
package com.fortify.cli.sc_dast.entity.scan.helper;

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
