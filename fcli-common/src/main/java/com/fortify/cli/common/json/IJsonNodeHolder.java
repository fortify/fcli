package com.fortify.cli.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface IJsonNodeHolder {
    void setJsonNode(JsonNode jsonNode);
    JsonNode asJsonNode();
    ObjectNode asObjectNode();
    ArrayNode asArrayNode();
}
