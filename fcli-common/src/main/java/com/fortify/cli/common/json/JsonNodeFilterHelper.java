package com.fortify.cli.common.json;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * @deprecated Command implementations should implement IDefaultOutputFieldsSupplier or IDefaultOutputColumnsSupplier instead, others should use com.fortify.cli.common.json.transform.fields.PredefinedFieldsTransformer 
 */
@Deprecated(forRemoval = true)
public class JsonNodeFilterHelper {

    public static void filterJsonNode (JsonNode node, Set<String> outputFields){
        Iterator<Map.Entry<String, JsonNode>> nodeFields = node.fields();
        while (nodeFields.hasNext()) {
            Map.Entry<String, JsonNode> nodeField = nodeFields.next();
            if( !outputFields.contains(nodeField.getKey())) { nodeFields.remove(); }
        }
    }
}
