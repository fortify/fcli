package com.fortify.cli.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    
    @Override
    public ObjectNode asObjectNode() {
        if ( !(jsonNode instanceof ObjectNode) ) { throw new IllegalStateException("JsonNode is not an instance of ObjectNode"); }
        return (ObjectNode)jsonNode;
    }
    
    @Override
    public ArrayNode asArrayNode() {
        if ( !(jsonNode instanceof ArrayNode) ) { throw new IllegalStateException("JsonNode is not an instance of ArrayNode"); }
        return (ArrayNode)jsonNode;
    }
    
    
}
