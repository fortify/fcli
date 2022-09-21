package com.fortify.cli.ssc.util;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;

public class SSCOutputConfigHelper {
    public static final UnaryOperator<JsonNode> GET_DATA = json->json.get("data");
    
    /**
     * Provide default table output configuration for results embedded in a data object
     * @return {@link OutputConfig}
     */
    public static final OutputConfig tableFromData() {
        return tableFromObjects().inputTransformer(GET_DATA);
    }
    
    /**
     * Provide default table output configuration for raw objects, not embedded in a data object
     * @return {@link OutputConfig}
     */
    public static final OutputConfig tableFromObjects() {
        return OutputConfig.table();
    }
    
    /**
     * Provide default details output configuration for results embedded in a data object
     * @return {@link OutputConfig}
     */
    public static final OutputConfig detailsFromData() {
        // TODO For now we use yaml output, until #104 has been fixed
        return detailsFromObjects().inputTransformer(GET_DATA);
    }
    
    /**
     * Provide default details output configuration for raw objects, not embedded in a data object
     * @return {@link OutputConfig}
     */
    public static final OutputConfig detailsFromObjects() {
        // TODO For now we use yaml output, until #104 has been fixed
        return OutputConfig.yaml();
    }
}
