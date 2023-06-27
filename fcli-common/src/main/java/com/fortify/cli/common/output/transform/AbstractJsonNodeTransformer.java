/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.output.transform;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractJsonNodeTransformer implements IJsonNodeTransformer {
    private final boolean supportNestedArrays;
    
    @Override
    public final JsonNode transform(JsonNode input) {
        return transform(input, this::transformObjectNode, this::transformArrayNode);
    }
    
    protected final JsonNode transform(JsonNode input, Function<ObjectNode,JsonNode> objectTransformer, Function<ArrayNode,JsonNode> arrayTransformer) {
        if ( input==null ) { return null; }
        if ( input instanceof ObjectNode ) {
            return objectTransformer.apply((ObjectNode)input);
        } else if ( input instanceof ArrayNode ) {
            return arrayTransformer.apply((ArrayNode)input);
        } else {
            return transformNonObjectOrArrayNode(input);
        }
    }
    
    protected final ArrayNode transformArrayElements(ArrayNode input, Function<ObjectNode,JsonNode> objectTransformer, Function<ArrayNode,JsonNode> arrayTransformer) {
        ArrayNode output = new ArrayNode(JsonNodeFactory.instance);
        input.forEach(jsonNode->output.add(transform(jsonNode, objectTransformer, arrayTransformer)));
        return output;
    }

    protected JsonNode transformArrayNode(ArrayNode input) {
        return transformArrayElements(input, this::transformObjectNode, this::transformNestedArrayNode);
    }
    
    protected JsonNode transformNestedArrayNode(ArrayNode input) {
        if ( !supportNestedArrays ) {
            throw new IllegalArgumentException("Nested arrays are not supported");
        }
        return transformArrayNode(input);
    }
    
    protected JsonNode transformNonObjectOrArrayNode(JsonNode input) {
        throw new IllegalArgumentException("Unsupported input type: "+input.getClass().getName());
    }

    protected abstract JsonNode transformObjectNode(ObjectNode input);
}