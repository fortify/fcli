package com.fortify.cli.ssc.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

public class SSCOutputConfigHelper {
    /**
     * Provide default table output configuration for results optionally embedded in a data object
     * @return {@link StandardOutputConfig}
     */
    public static final StandardOutputConfig table() {
        return StandardOutputConfig.table().inputTransformer(SSCOutputConfigHelper::getDataOrSelf);
    }
    
    /**
     * Provide default details output configuration for results optionally embedded in a data object
     * @return {@link StandardOutputConfig}
     */
    public static final StandardOutputConfig details() {
        // TODO For now we use yaml output, until #104 has been fixed
        return StandardOutputConfig.yaml().inputTransformer(SSCOutputConfigHelper::getDataOrSelf);
    }
    
    private static final JsonNode getDataOrSelf(JsonNode json) {
        return json.has("data") ? json.get("data") : json;
    }
}
