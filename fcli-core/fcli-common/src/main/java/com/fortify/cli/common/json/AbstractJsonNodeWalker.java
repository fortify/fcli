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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

public abstract class AbstractJsonNodeWalker<R, S> {
    public final R walk(JsonNode node) {
        if (node != null) {
            walk(null, "", null, node);
        }
        return getResult();
    }
    protected abstract R getResult();

    protected void walk(S state, String path, JsonNode parent, JsonNode node) {
        if (!skipNode(state, path, parent, node)) {
            if (node instanceof ContainerNode) {
                walkContainer(state, path, parent, (ContainerNode<?>) node);
            } else if (node instanceof ValueNode) {
                walkValue(state, path, parent, (ValueNode) node);
            }
        }
    }

    protected boolean skipNode(S state, String path, JsonNode parent, JsonNode node) {
        return false;
    }

    protected void walkContainer(S state, String path, JsonNode parent, ContainerNode<?> node) {
        if (node instanceof ArrayNode) {
            walkArray(state, path, parent, (ArrayNode) node);
        } else if (node instanceof ObjectNode) {
            walkObject(state, path, parent, (ObjectNode) node);
        }
    }

    protected void walkObject(S state, String path, JsonNode parent, ObjectNode node) {
        node.fields().forEachRemaining(e -> walkObjectProperty(state, appendPath(path, e.getKey()), node, e.getKey(), e.getValue()));
    }

    protected void walkObjectProperty(S state, String path, ObjectNode parent, String property, JsonNode value) {
        walk(state, path, parent, value);
    }

    protected void walkArray(S state, String path, JsonNode parent, ArrayNode node) {
        for (int i = 0; i < node.size(); i++) {
            walkArrayElement(state, appendPath(path, i + ""), node, i, node.get(i));
        }
    }

    protected void walkArrayElement(S state, String path, ArrayNode parent, int index, JsonNode value) {
        walk(state, path, parent, value);
    }

    protected void walkValue(S state, String path, JsonNode parent, ValueNode node) {
    }

    protected final String appendPath(String parent, String entry) {
        return String.format("%s[%s]", parent, entry);
    }
}
