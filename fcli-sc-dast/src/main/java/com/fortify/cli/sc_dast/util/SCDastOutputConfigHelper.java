package com.fortify.cli.sc_dast.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;

public class SCDastOutputConfigHelper {
    /**
     * Provide default table output configuration for results optionally embedded in an item or items object
     * @return {@link OutputConfig}
     */
    public static final OutputConfig table() {
        return OutputConfig.table().inputTransformer(SCDastOutputConfigHelper::getItems);
    }
    
    /**
     * Provide default details output configuration for results optionally embedded in an item or items object
     * @return {@link OutputConfig}
     */
    public static final OutputConfig details() {
        // TODO For now we use yaml output, until #104 has been fixed
        return OutputConfig.yaml().inputTransformer(SCDastOutputConfigHelper::getItems);
    }
    
    private static final JsonNode getItems(JsonNode input) {
        if ( input.has("items") ) { return input.get("items"); }
        if ( input.has("item") ) { return input.get("item"); }
        return input;
    }
}
