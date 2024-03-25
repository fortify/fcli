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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fortify.cli.common.json.JsonHelper.JsonNodeDeepCopyWalker;

public class JsonNodeDeepCopyWalkerTest {
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    
    @ParameterizedTest
    @MethodSource("getSampleNodesStream")
    public void testDeepCopy(JsonNode node) throws Exception {
        var copy = new JsonNodeDeepCopyWalker().walk(node);
        assertEquals(node, copy);
        if ( node!=null && !(node instanceof ValueNode) ) {
            System.out.println(copy.toPrettyString());
            assertFalse(node==copy);
        }
    }
    
    private static Stream<Arguments> getSampleNodesStream() {
        return Stream.of(
            Arguments.of(new Object[] {null}),
            Arguments.of(createEmptyObjectNode()),
            Arguments.of(createSampleValueObjectNode()),
            Arguments.of(createSampleMultiLevelObjectNode()),
            Arguments.of(createEmptyArrayNode()),
            Arguments.of(createSampleValueArrayNode()),
            Arguments.of(createSampleMultiLevelArrayNode()),
            Arguments.of(createSampleValueNode())
        );
    }
    
    private static ObjectNode createEmptyObjectNode() {
        return objectMapper.createObjectNode();
    }
    
    private static ArrayNode createEmptyArrayNode() {
        return objectMapper.createArrayNode();
    }
    
    private static ObjectNode createSampleMultiLevelObjectNode() {
        var result = createSampleValueObjectNode();
        result.set("array", createSampleValueArrayNode());
        result.set("obj", createSampleValueObjectNode());
        return result;
    }
    
    private static ObjectNode createSampleValueObjectNode() {
        var result = objectMapper.createObjectNode();
        result.put("int", 1);
        result.put("bool", true);
        result.put("float", 2.2f);
        result.put("str1", "someString");
        result.set("val", createSampleValueNode());
        return result;
    }
    
    private static ArrayNode createSampleMultiLevelArrayNode() {
        var result = objectMapper.createArrayNode();
        for ( int i = 0 ; i < 20 ; i++ ) {
            result.add(createSampleMultiLevelObjectNode());
        }
        return result;
    }
    
    private static ArrayNode createSampleValueArrayNode() {
        var result = objectMapper.createArrayNode();
        for ( int i = 0 ; i < 10 ; i++ ) {
            result.add(100+i);
        }
        return result;
    }
    
    private static ValueNode createSampleValueNode() {
        return new TextNode(UUID.randomUUID().toString());
    }
    
}
