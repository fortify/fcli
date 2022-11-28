package com.fortify.cli.common.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Override @JsonIgnore
    public JsonNode asJsonNode() {
        if ( jsonNode==null ) {
            jsonNode = JsonHelper.getObjectMapper().valueToTree(this);
        }
        return jsonNode;
    }
    
    @Override @JsonIgnore
    public ObjectNode asObjectNode() {
        if ( !(jsonNode instanceof ObjectNode) ) { throw new IllegalStateException("JsonNode is not an instance of ObjectNode"); }
        return (ObjectNode)jsonNode;
    }
    
    @Override @JsonIgnore
    public ArrayNode asArrayNode() {
        if ( !(jsonNode instanceof ArrayNode) ) { throw new IllegalStateException("JsonNode is not an instance of ArrayNode"); }
        return (ArrayNode)jsonNode;
    }
    
    
}
