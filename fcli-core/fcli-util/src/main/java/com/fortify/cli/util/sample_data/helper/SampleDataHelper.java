/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.util.sample_data.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import lombok.Getter;

public final class SampleDataHelper {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Getter(lazy = true)
    private static final ArrayNode sampleData = generateSampleData();
    
    // IMPORTANT: Any changes to this method may have an impact on fcli 
    //            functional tests, potentially causing test failures.
    //            After updating this method, please run the core functional 
    //            tests and adjust to match the sample data changes if 
    //            necessary.
    private static final ArrayNode generateSampleData() {
        ArrayNode result = objectMapper.createArrayNode();
        var id = 0;
        for ( var stringValue : stringValues() ) {
            for ( var longValue : longValues() ) {
                for ( var doubleValue : doubleValues() ) {
                    for ( var booleanValue : booleanValues() ) {
                        for ( var dateValue : dateValues() ) {
                            for ( var dateTimeValue : dateTimeValues() ) {
                                for ( var nestedObject : nestedObjectValues() ) {
                                    for ( var nestedObjectArray : nestedObjectArrayValues() ) {
                                        for ( var nestedStringArray : nestedStringArrayValues() ) {
                                            var obj = objectMapper.createObjectNode()
                                                    .put("id", id++)
                                                    .put("stringValue", stringValue)
                                                    .put("longValue", longValue)
                                                    .put("doubleValue", doubleValue)
                                                    .put("booleanValue", booleanValue)
                                                    .put("dateValue", dateValue)
                                                    .put("dateTimeValue", dateTimeValue);
                                            obj.set("nestedObject", nestedObject);
                                            obj.set("nestedObjectArray", nestedObjectArray);
                                            obj.set("nestedStringArray", nestedStringArray);
                                            result.add(obj);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
    
    private static final List<String> stringValues() {
        var result = new ArrayList<String>();
        for ( int i = 1; i <= 2 ; i++ ) {
            result.add("value"+i);
        }
        result.add(null);
        return result;
    }
    
    private static final List<Long> longValues() {
        var result = new ArrayList<Long>();
        for ( long i = 1; i <= 2 ; i++ ) {
            result.add(i*1000);
            result.add(-i*1000);
        }
        result.add(Long.MAX_VALUE);
        result.add(null);
        return result;
    }
    
    private static final List<Double> doubleValues() {
        var result = new ArrayList<Double>();
        for ( int i = 1; i <= 2 ; i++ ) {
            result.add(i*0.7);
            result.add(-i*0.7);
        }
        result.add(Double.MAX_VALUE);
        result.add(null);
        return result;
    }
    
    private static final List<Boolean> booleanValues() {
        return Arrays.asList(true, false, null);
    }
    
    private static final List<String> dateValues() {
        return Arrays.asList("2000-01-01", "2030-12-31", null);
    }
    
    private static final List<String> dateTimeValues() {
        return Arrays.asList("2000-01-01T00:00:00+00:00", "2030-12-31T23:59:59+02:00", null);
    }
    
    private static final List<ObjectNode> nestedObjectValues() {
        var result = new ArrayList<ObjectNode>();
        result.add(objectMapper.createObjectNode()
                .put("stringValue", "nestedObjectValue1")
                .put("booleanValue", true));
        result.add(null);
        return result;
    }
    
    private static final List<ArrayNode> nestedObjectArrayValues() {
        var result = new ArrayList<ArrayNode>();
        result.add(objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode()
                        .put("stringValue", "nestedArrayValue1")
                        .put("booleanValue", true))
                .add(objectMapper.createObjectNode()
                        .put("stringValue", "nestedArrayValue2")
                        .put("booleanValue", false)));
        result.add(null);
        return result;
    }
    
    private static final List<ArrayNode> nestedStringArrayValues() {
        var result = new ArrayList<ArrayNode>();
        result.add(objectMapper.createArrayNode()
                .add(new TextNode("nestedArrayValue3"))
                .add(new TextNode("nestedArrayValue4")));
        result.add(null);
        return result;
    }
}
