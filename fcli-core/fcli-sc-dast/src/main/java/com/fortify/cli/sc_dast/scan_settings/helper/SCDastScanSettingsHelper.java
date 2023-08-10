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
package com.fortify.cli.sc_dast.scan_settings.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.core.UnirestInstance;

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
