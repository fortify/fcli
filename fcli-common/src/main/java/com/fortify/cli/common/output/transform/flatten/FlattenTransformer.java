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
package com.fortify.cli.common.output.transform.flatten;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
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
            } else if (node.isArray()) {
                ArrayNode array = (ArrayNode) node;
                JsonNodeType nodeType = array==null || array.isEmpty() ? null : array.get(0).getNodeType();
                if ( nodeType!=null ) {
                    switch (nodeType) {
                    case ARRAY: case OBJECT: case POJO: flattenNestedArray(array, prefix); break;
                    case STRING: case NUMBER: result.put(fieldNameFormatter.apply(prefix), toConcatenatedString(array)); break;
                    default: // TODO Ignore all others?
                    }
                }
            } else {
                result.set(fieldNameFormatter.apply(prefix), node);
            }
        }
        
        private void flattenNestedArray(ArrayNode array, String prefix) {
            if ( flattenNestedArrays ) {
                AtomicInteger counter = new AtomicInteger();
                array.elements().forEachRemaining(item -> {
                    flatten(item, getPrefix(prefix, counter.getAndIncrement()));
                });
            }
        }
        
        private String toConcatenatedString(ArrayNode array) {
            return JsonHelper.stream(array).map(JsonNode::textValue).collect(Collectors.joining(", "));
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
