/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
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

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.expression.Expression;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fortify.cli.common.spring.expression.SpelEvaluator;
import com.fortify.cli.common.util.StringUtils;

import lombok.Getter;

/**
 * This bean provides utility methods for working with Jackson JsonNode trees.
 * 
 * @author Ruud Senden
 *
 */
public class JsonHelper {
    @Getter private static final ObjectMapper objectMapper = _createObjectMapper();
    //private static final Logger LOG = LoggerFactory.getLogger(JsonHelper.class);
    private static final ObjectMapper _createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
    
    public static final <R> R evaluateSpelExpression(JsonNode input, Expression expression, Class<R> returnClass) {
        return SpelEvaluator.JSON_GENERIC.evaluate(expression, input, returnClass);
    }

    public static final <R> R evaluateSpelExpression(JsonNode input, String expression, Class<R> returnClass) {
        return SpelEvaluator.JSON_GENERIC.evaluate(expression, input, returnClass);
    }
    
    public static final Iterable<JsonNode> iterable(ArrayNode arrayNode) {
        Iterator<JsonNode> iterator = arrayNode.iterator();
        return () -> iterator;
    }
    
    public static final Stream<JsonNode> stream(ArrayNode arrayNode) {
        return StreamSupport.stream(iterable(arrayNode).spliterator(), false);
    }
    
    public static final ObjectNode shallowCopy(ObjectNode node) {
        var newData = objectMapper.createObjectNode();
        newData.setAll(node);
        return newData;
    }
    
    public static final ArrayNodeCollector arrayNodeCollector() {
        return new ArrayNodeCollector();
    }
    
    public static final ArrayNode toArrayNode(String... objects) {
        return Stream.of(objects).map(TextNode::new).collect(arrayNodeCollector());
    }
    
    public static final ArrayNode toArrayNode(JsonNode... objects) {
        return Stream.of(objects).collect(arrayNodeCollector());
    }
    
    public static <T> T treeToValue(JsonNode node, Class<T> returnType) {
        if ( node==null ) { return null; }
        try {
            T result = objectMapper.treeToValue(node, returnType);
            if ( result instanceof IJsonNodeHolder ) {
                ((IJsonNodeHolder)result).setJsonNode(node);
            }
            return result;
        } catch (JsonProcessingException jpe ) {
            throw new RuntimeException("Error processing JSON data", jpe);
        }
    }
    
    public static <T> T jsonStringToValue(String jsonString, Class<T> returnType) {
        if ( StringUtils.isBlank(jsonString) ) { return null; }
        try {
            return treeToValue(objectMapper.readTree(jsonString), returnType);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException("Error processing JSON data", jpe);
        }
    }
    
    private static final class ArrayNodeCollector implements Collector<JsonNode, ArrayNode, ArrayNode> {
        @Override
        public Supplier<ArrayNode> supplier() {
            return objectMapper::createArrayNode;
        }

        @Override
        public BiConsumer<ArrayNode, JsonNode> accumulator() {
            return ArrayNode::add;
        }

        @Override
        public BinaryOperator<ArrayNode> combiner() {
            return (x, y) -> {
                x.addAll(y);
                return x;
            };
        }

        @Override
        public Function<ArrayNode, ArrayNode> finisher() {
            return accumulator -> accumulator;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return EnumSet.of(Characteristics.UNORDERED);
        }
    }
    
    public static abstract class AbstractJsonNodeWalker<R, S> {
        public final R walk(JsonNode node) {
            if ( node!=null ) {
                walk(null, "", null, node);
            }
            return getResult();
        }
        protected abstract R getResult();
        
        protected void walk(S state, String path, JsonNode parent, JsonNode node) {
            if ( !skipNode(state, path, parent, node) ) {
                if ( node instanceof ContainerNode ) {
                    walkContainer(state, path, parent, (ContainerNode<?>)node);
                } else if ( node instanceof ValueNode ) {
                    walkValue(state, path, parent, (ValueNode)node);
                }
            }
        }
        
        protected boolean skipNode(S state, String path, JsonNode parent, JsonNode node) {
            return false;
        }
        
        protected void walkContainer(S state, String path, JsonNode parent, ContainerNode<?> node) {
            if ( node instanceof ArrayNode ) {
                walkArray(state, path, parent, (ArrayNode)node);
            } else if ( node instanceof ObjectNode ) {
                walkObject(state, path, parent, (ObjectNode)node);
            }
        }
        
        protected void walkObject(S state, String path, JsonNode parent, ObjectNode node) {
            node.fields().forEachRemaining(e->walkObjectProperty(state, appendPath(path, e.getKey()), node, e.getKey(), e.getValue()));
        }
        
        protected void walkObjectProperty(S state, String path, ObjectNode parent, String property, JsonNode value) {
            walk(state, path, parent, value);
        }
        
        protected void walkArray(S state, String path, JsonNode parent, ArrayNode node) {
            for ( int i = 0 ; i < node.size() ; i++ ) {
                walkArrayElement(state, appendPath(path, i+""), node, i, node.get(i));
            }
        }
        
        protected void walkArrayElement(S state, String path, ArrayNode parent, int index, JsonNode value) {
            walk(state, path, parent, value);
        }
        
        protected void walkValue(S state, String path, JsonNode parent, ValueNode node) {}
        
        protected final String appendPath(String parent, String entry) {
            return String.format("%s[%s]", parent, entry);
        }
    }
    
    public static class JsonNodeDeepCopyWalker extends AbstractJsonNodeWalker<JsonNode, JsonNode> {
        @Getter JsonNode result;
        @Override
        protected void walkObject(JsonNode state, String path, JsonNode parent, ObjectNode node) {
            if ( state==null ) { state = objectMapper.createObjectNode(); }
            if ( result==null ) { result = state; }
            super.walkObject(state, path, parent, node);
        }
        @Override
        protected void walkObjectProperty(JsonNode state, String path, ObjectNode parent, String property, JsonNode value) {
            if ( value instanceof ContainerNode ) {
                var newState = createContainerNode(value.getNodeType());
                ((ObjectNode)state).set(property, newState);
                super.walkObjectProperty(newState, path, parent, property, value);
            } else {
                ((ObjectNode)state).set(property, copyValue(state, path, parent, (ValueNode)value));
            }
        }
        @Override
        protected void walkArray(JsonNode state, String path, JsonNode parent, ArrayNode node) {
            if ( state==null ) { state = objectMapper.createArrayNode(); }
            if ( result==null ) { result = state; }
            super.walkArray(state, path, parent, node);
        }
        @Override
        protected void walkArrayElement(JsonNode state, String path, ArrayNode parent, int index, JsonNode value) {
            if ( value instanceof ContainerNode ) {
                var newState = createContainerNode(value.getNodeType());
                ((ArrayNode)state).insert(index, newState);
                super.walkArrayElement(newState, path, parent, index, value);
            } else {
                ((ArrayNode)state).insert(index, copyValue(state, path, parent, (ValueNode)value));
            }
        }
        @Override
        protected void walkValue(JsonNode state, String path, JsonNode parent, ValueNode node) {
            if ( result == null ) { result = copyValue(state, path, parent, node); }
        }
        protected final JsonNode createContainerNode(JsonNodeType jsonNodeType) {
            return jsonNodeType==JsonNodeType.ARRAY 
                    ? objectMapper.createArrayNode() 
                    : objectMapper.createObjectNode();
        }
        // We return JsonNode to allow subclasses to return other node types
        protected JsonNode copyValue(JsonNode state, String path, JsonNode parent, ValueNode node) {
            return node.deepCopy();
        }
    }
}
