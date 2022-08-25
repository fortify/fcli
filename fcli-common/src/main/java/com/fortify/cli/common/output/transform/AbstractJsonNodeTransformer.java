/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
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