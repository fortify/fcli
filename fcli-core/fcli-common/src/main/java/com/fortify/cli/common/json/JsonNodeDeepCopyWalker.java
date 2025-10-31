/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import lombok.Getter;

public class JsonNodeDeepCopyWalker extends AbstractJsonNodeWalker<JsonNode, JsonNode> {
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    @Getter
    JsonNode result;
    @Override
    protected void walkObject(JsonNode state, String path, JsonNode parent, ObjectNode node) {
        if (state == null) {
            state = objectMapper.createObjectNode();
        }
        if (result == null) {
            result = state;
        }
        super.walkObject(state, path, parent, node);
    }
    @Override
    protected void walkObjectProperty(JsonNode state, String path, ObjectNode parent, String property, JsonNode value) {
        if (value instanceof ContainerNode) {
            var newState = createContainerNode(value.getNodeType());
            ((ObjectNode) state).set(property, newState);
            super.walkObjectProperty(newState, path, parent, property, value);
        } else {
            ((ObjectNode) state).set(property, copyValue(state, path, parent, (ValueNode) value));
        }
    }
    @Override
    protected void walkArray(JsonNode state, String path, JsonNode parent, ArrayNode node) {
        if (state == null) {
            state = objectMapper.createArrayNode();
        }
        if (result == null) {
            result = state;
        }
        super.walkArray(state, path, parent, node);
    }
    @Override
    protected void walkArrayElement(JsonNode state, String path, ArrayNode parent, int index, JsonNode value) {
        if (value instanceof ContainerNode) {
            var newState = createContainerNode(value.getNodeType());
            ((ArrayNode) state).insert(index, newState);
            super.walkArrayElement(newState, path, parent, index, value);
        } else {
            ((ArrayNode) state).insert(index, copyValue(state, path, parent, (ValueNode) value));
        }
    }
    @Override
    protected void walkValue(JsonNode state, String path, JsonNode parent, ValueNode node) {
        if (result == null) {
            result = copyValue(state, path, parent, node);
        }
    }
    protected final JsonNode createContainerNode(JsonNodeType jsonNodeType) {
        return jsonNodeType == JsonNodeType.ARRAY ? objectMapper.createArrayNode() : objectMapper.createObjectNode();
    }
    // We return JsonNode to allow subclasses to return other node types
    protected JsonNode copyValue(JsonNode state, String path, JsonNode parent, ValueNode node) {
        return node.deepCopy();
    }
}
