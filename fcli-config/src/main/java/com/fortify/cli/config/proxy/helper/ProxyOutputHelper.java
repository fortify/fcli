package com.fortify.cli.config.proxy.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ProxyOutputHelper {
    private ProxyOutputHelper() {}
    
    public static final JsonNode transformRecord(JsonNode record) {
        ObjectNode node = ((ObjectNode)record);
        node.put("proxyPassword", "****"); // Hide proxy password in any output
        return node;
    }
}
