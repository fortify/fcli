package com.fortify.cli.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JacksonJsonNodeHelper;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * TODO This class should move under the com.fortify.cli.common.json package
 * TODO Functionality provided by this class should potentially be integrated into {@link JacksonJsonNodeHelper}
 */
public class JsonNodeFilterHandler {

    public static JsonNode filterJsonNode (JsonNode node, Set<String> outputFields){
        Iterator<Map.Entry<String, JsonNode>> nodeFields = node.fields();
        while (nodeFields.hasNext()) {
            Map.Entry<String, JsonNode> nodeField = nodeFields.next();
            if( !outputFields.contains(nodeField.getKey())) { nodeFields.remove(); }
        }

        return node;
    }
}
