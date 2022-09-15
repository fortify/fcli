package com.fortify.cli.common.util;

import com.fasterxml.jackson.databind.JsonNode;

public interface IJsonNodeHolder {
    void setJsonNode(JsonNode jsonNode);
    JsonNode asJsonNode();
}
