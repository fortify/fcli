package com.fortify.cli.sc_sast.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

public class SCSastOutputHelper {
    public static final StandardOutputConfig defaultTableOutputConfig() {
        return StandardOutputConfig.table().inputTransformer(SCSastOutputHelper::getItems);
    }
    
    private static final JsonNode getItems(JsonNode input) {
        // TODO Get actual contents
        return input;
    }
}
