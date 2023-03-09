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

package com.fortify.cli.fod.dast_scan.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod.release.helper.FoDAppRelDescriptor;
import com.fortify.cli.fod.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDScanNotFoundException;
import com.fortify.cli.fod.scan.helper.FoDStartScanResponse;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDDastScanHelper extends FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /*public static final FoDDastScanSetupDescriptor setupScan(UnirestInstance unirest, Integer relId, FoDSetupDastScanRequest setupDastScanRequest) {
        ObjectNode body = objectMapper.valueToTree(setupDastScanRequest);
        FoDQueryHelper.stripNulls(body);
        System.out.println(body.toPrettyString());
        JsonNode response = unirest.put(FoDUrls.DYNAMIC_SCANS + "/scan-setup")
                .routeParam("relId", String.valueOf(relId))
                .body(body).asObject(JsonNode.class).getBody();
        return JsonHelper.treeToValue(response, FoDDastScanSetupDescriptor.class);
    }*/

    public static final FoDScanDescriptor startScan(UnirestInstance unirest, String relId, FoDStartDastScanRequest startDastScanRequest) {
        ObjectNode body = objectMapper.valueToTree(startDastScanRequest);
        FoDAppRelDescriptor appRelDescriptor = FoDAppRelHelper.getAppRelDescriptor(unirest, relId, ":", true);
        JsonNode response = unirest.post(FoDUrls.DYNAMIC_SCANS + "/start-scan")
                .routeParam("relId", relId)
                .body(body).asObject(JsonNode.class).getBody();
        FoDStartScanResponse startScanResponse = JsonHelper.treeToValue(response, FoDStartScanResponse.class);
        if (startScanResponse == null || startScanResponse.getScanId() <= 0) {
            throw new RuntimeException("Unable to retrieve scan id from response when starting Dynamic scan.");
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

    public static final FoDDastScanSetupDescriptor getSetupDescriptor(UnirestInstance unirest, String relId) {
        GetRequest request = unirest.get(FoDUrls.DYNAMIC_SCANS + "/scan-setup")
                .routeParam("relId", relId);
        JsonNode setup = request.asObject(ObjectNode.class).getBody();
        return JsonHelper.treeToValue(setup, FoDDastScanSetupDescriptor.class);
    }

}
