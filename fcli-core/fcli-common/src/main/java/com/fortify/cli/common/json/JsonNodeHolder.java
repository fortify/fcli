/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
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
        var node = asJsonNode();
        if ( !(node instanceof ObjectNode) ) { throw new IllegalStateException("JsonNode is not an instance of ObjectNode"); }
        return (ObjectNode)node;
    }
    
    @Override @JsonIgnore
    public ArrayNode asArrayNode() {
        var node = asJsonNode();
        if ( !(node instanceof ArrayNode) ) { throw new IllegalStateException("JsonNode is not an instance of ArrayNode"); }
        return (ArrayNode)node;
    }
    
    
}
