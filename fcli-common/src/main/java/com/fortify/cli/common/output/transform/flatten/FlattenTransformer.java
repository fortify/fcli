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
package com.fortify.cli.common.output.transform.flatten;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.transform.AbstractJsonNodeTransformer;

import io.micronaut.core.util.StringUtils;
import lombok.RequiredArgsConstructor;

public class FlattenTransformer extends AbstractJsonNodeTransformer {
    private final Function<String, String> fieldNameFormatter;
    private final String separator;
    private final boolean flattenNestedArrays;

    public FlattenTransformer(Function<String, String> fieldNameFormatter, String separator, boolean flattenNestedArrays) {
        super(false);
        this.fieldNameFormatter = fieldNameFormatter;
        this.separator = separator;
        this.flattenNestedArrays = flattenNestedArrays;
    }

    @Override
    public ObjectNode transformObjectNode(ObjectNode input) {
        return new ObjectNodeFlattener(input, fieldNameFormatter, separator, flattenNestedArrays).flatten();
    }

    @RequiredArgsConstructor
    private final class ObjectNodeFlattener {
        private final ObjectNode root;
        private final Function<String, String> fieldNameFormatter;
        private final String separator;
        private final boolean flattenNestedArrays;
        private ObjectNode result = null;

        public ObjectNode flatten() {
            if (result == null) {
                result = new ObjectNode(JsonNodeFactory.instance);
                flatten(root, "");
            }
            return result;
        }

        private void flatten(JsonNode node, String prefix) {
            if (node.isObject()) {
                ObjectNode object = (ObjectNode) node;
                object.fields().forEachRemaining(entry -> {
                    flatten(entry.getValue(), getPrefix(prefix, entry.getKey()));
                });
            } else if (node.isArray() && flattenNestedArrays) {
                ArrayNode array = (ArrayNode) node;
                AtomicInteger counter = new AtomicInteger();
                array.elements().forEachRemaining(item -> {
                    flatten(item, getPrefix(prefix, counter.getAndIncrement()));
                });
            } else {
                result.set(fieldNameFormatter.apply(prefix), node);
            }
        }

        private String getPrefix(String prefix, String key) {
            return StringUtils.isEmpty(prefix) 
                    ? key
                    : (prefix + separator + key);
        }
        
        private String getPrefix(String prefix, int count) {
            return StringUtils.isEmpty(prefix) 
                    ? String.valueOf(count)
                    : (prefix + separator + count);
        }

    }
}
