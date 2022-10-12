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
package com.fortify.cli.sc_dast.scan_settings.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.UnirestInstance;

public class SCDastScanSettingsHelper {
    private SCDastScanSettingsHelper() {}
    
    public static final SCDastScanSettingsDescriptor getScanSettingsDescriptor(UnirestInstance unirest, String scanSettingsCicdTokenOrId) {
        JsonNode settings = null;
        try {
            int scanSettingsId = Integer.parseInt(scanSettingsCicdTokenOrId);
            settings = unirest.get(String.format("/api/v2/application-version-scan-settings/%s", scanSettingsId)).asObject(JsonNode.class).getBody();
        } catch ( NumberFormatException nfe ) {
            settings = JsonHelper.stream(
                (ArrayNode)unirest.get("/api/v2/application-version-scan-settings/scan-settings-summary-list").asObject(JsonNode.class).getBody().get("items")
            )
                    .filter(n->n.get("cicdToken").asText().equals(scanSettingsCicdTokenOrId))
                    .findFirst()
                    .orElseThrow(()->new IllegalArgumentException("Cicd token "+scanSettingsCicdTokenOrId+" not found"));
        }
        return getDescriptor(settings);
    }

    private static final SCDastScanSettingsDescriptor getDescriptor(JsonNode sensorNode) {
        return JsonHelper.treeToValue(sensorNode, SCDastScanSettingsDescriptor.class);
    }
}
