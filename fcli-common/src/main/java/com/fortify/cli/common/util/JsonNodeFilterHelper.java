package com.fortify.cli.common.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JsonNodeFilterHelper {

    public static void filterJsonNode (JsonNode node, Set<String> outputFields){
        Iterator<Map.Entry<String, JsonNode>> nodeFields = node.fields();
        while (nodeFields.hasNext()) {
            Map.Entry<String, JsonNode> nodeField = nodeFields.next();
            if( !outputFields.contains(nodeField.getKey())) { nodeFields.remove(); }
        }
    }
}
