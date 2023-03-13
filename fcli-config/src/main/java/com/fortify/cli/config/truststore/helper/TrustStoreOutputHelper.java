package com.fortify.cli.config.truststore.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TrustStoreOutputHelper {
    private TrustStoreOutputHelper() {}
    
    public static final JsonNode transformRecord(JsonNode record) {
        ObjectNode node = ((ObjectNode)record);
        node.put("password", "****"); // Hide trust store password in any output
        return node;
    }
}
