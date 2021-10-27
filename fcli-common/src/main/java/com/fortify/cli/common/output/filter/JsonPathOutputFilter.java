package com.fortify.cli.common.output.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;

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
