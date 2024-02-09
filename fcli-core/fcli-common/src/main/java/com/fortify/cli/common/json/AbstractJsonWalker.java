/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
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

public abstract class AbstractJsonWalker<R> {
    public R walk(JsonNode node) {
        walk("", null, node);
        return getResult();
    }
    protected abstract R getResult();
    
    protected void walk(String path, JsonNode parent, JsonNode node) {
        if ( !skipNode(path, parent, node) ) {
            if ( node instanceof ContainerNode ) {
                walkContainer(path, parent, (ContainerNode<?>)node);
            } else if ( node instanceof ValueNode ) {
                walkValue(path, parent, (ValueNode)node);
            }
        }
    }
    
    protected boolean skipNode(String path, JsonNode parent, JsonNode node) {
        return false;
    }
    
    protected void walkContainer(String path, JsonNode parent, ContainerNode<?> node) {
        if ( node instanceof ArrayNode ) {
            walkArray(path, parent, (ArrayNode)node);
        } else if ( node instanceof ObjectNode ) {
            walkObject(path, parent, (ObjectNode)node);
        }
    }
    
    protected void walkObject(String path, JsonNode parent, ObjectNode node) {
        node.fields().forEachRemaining(e->walkObjectProperty(appendPath(path, e.getKey()), node, e.getKey(), e.getValue()));
    }
    
    protected void walkObjectProperty(String path, ObjectNode parent, String property, JsonNode value) {
        walk(path, parent, value);
    }
    
    protected void walkArray(String path, JsonNode parent, ArrayNode node) {
        for ( int i = 0 ; i < node.size() ; i++ ) {
            walkArrayElement(appendPath(path, i+""), node, i, node.get(i));
        }
    }
    
    protected void walkArrayElement(String path, ArrayNode parent, int index, JsonNode value) {
        walk(path, parent, value);
    }
    
    protected void walkValue(String path, JsonNode parent, ValueNode node) {
        
    }
    
    protected final String appendPath(String parent, String entry) {
        return String.format("%s[%s]", parent, entry);
    }
}
