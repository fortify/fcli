package com.fortify.cli.common.json;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString @EqualsAndHashCode
public class JsonNodeHolder implements IJsonNodeHolder {
    private JsonNode jsonNode;
    @Override
    public void setJsonNode(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    @Override
    public JsonNode asJsonNode() {
        return jsonNode;
    }
}
