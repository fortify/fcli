package com.fortify.cli.common.output.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JacksonJsonNodeHelper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;

/**
 * TODO Move to the json or json.transform(er) package, as this can potentially also be used for other things than output processing
 * TODO Potentially this could also just be merged into {@link JacksonJsonNodeHelper}, as functionality is very similar to {@link JacksonJsonNodeHelper#getPath(Object, String, Class)}
 * TODO Clean up code; remove unused 'root' variable, and do we really need all the potentially expensive conversions (in particular converting jsonNode to a String and then parsing that into a DocumentContext) 
 * 
 */
public class JsonPathOutputFilter {
    @SneakyThrows
    public static JsonNode filter(JsonNode jsonNode, String expression) {
        ObjectMapper objectMapper = new ObjectMapper();

        DocumentContext jsonContext = JsonPath.parse(jsonNode.toString());

        ObjectNode root = objectMapper.createObjectNode();

        //root.set("value", );

        return objectMapper.valueToTree(jsonContext.read(expression));
    }
}
