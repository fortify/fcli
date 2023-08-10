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
package com.fortify.cli.ssc.plugin.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder;

import kong.unirest.core.UnirestInstance;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class SSCPluginStateHelper {
    public static final JsonNode enablePlugin(UnirestInstance unirest, String pluginId) {
        return postPluginId(unirest, SSCUrls.PLUGINS_ACTION_ENABLE, pluginId);
    }
    
    public static final JsonNode disablePlugin(UnirestInstance unirest, String pluginId) {
        return postPluginId(unirest, SSCUrls.PLUGINS_ACTION_DISABLE, pluginId);
    }
    
    private static final JsonNode postPluginId(UnirestInstance unirest, String endpoint, String pluginId) {
        return new SSCBulkRequestBuilder()
            .request("action", unirest.post(endpoint).body(new PluginIdsData(pluginId)))
            .request("pluginData", unirest.get(SSCUrls.PLUGIN(String.valueOf(pluginId))))
            .execute(unirest)
            .body("pluginData");
    }
    
    @Data 
    @Reflectable @NoArgsConstructor
    private static final class PluginIdsData {
        private int[] pluginIds = {};
        
        // Even though SSC expects an array of plugin id's, it only accepts a single plugin id
        private PluginIdsData(String pluginId) {
            this.pluginIds = new int[] {Integer.parseInt(pluginId)};
        }
    }
}
