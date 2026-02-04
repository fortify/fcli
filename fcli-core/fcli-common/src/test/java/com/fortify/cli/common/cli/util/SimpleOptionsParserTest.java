/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.common.cli.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.cli.util.SimpleOptionsParser.IOptionDescriptor;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionDescriptor;

public class SimpleOptionsParserTest {
    
    @Test
    public void testBooleanFlagFollowedByOptionWithEquals() {
        List<IOptionDescriptor> options = List.of(
            OptionDescriptor.builder()
                .id("use-aviator")
                .optionNames(new String[] {"--use-aviator"})
                .description("Use aviator")
                .bool(true)
                .build(),
            OptionDescriptor.builder()
                .id("release")
                .optionNames(new String[] {"--release", "--rel"})
                .description("Release name")
                .bool(false)
                .build()
        );
        
        var parser = new SimpleOptionsParser(options);
        var result = parser.parse(new String[] {"--use-aviator", "--rel=myrelease"});
        
        assertFalse(result.hasValidationErrors(), 
            "Should not have validation errors: " + result.getValidationErrors());
        assertEquals("true", result.getOptionValuesById().get("use-aviator"), 
            "Boolean flag should be set to true");
        assertEquals("myrelease", result.getOptionValuesById().get("release"), 
            "--rel should have value 'myrelease'");
    }
    
    @Test
    public void testBooleanFlagFollowedByOptionWithoutEquals() {
        List<IOptionDescriptor> options = List.of(
            OptionDescriptor.builder()
                .id("use-aviator")
                .optionNames(new String[] {"--use-aviator"})
                .description("Use aviator")
                .bool(true)
                .build(),
            OptionDescriptor.builder()
                .id("release")
                .optionNames(new String[] {"--release", "--rel"})
                .description("Release name")
                .bool(false)
                .build()
        );
        
        var parser = new SimpleOptionsParser(options);
        var result = parser.parse(new String[] {"--use-aviator", "--rel", "myrelease"});
        
        assertFalse(result.hasValidationErrors(), 
            "Should not have validation errors: " + result.getValidationErrors());
        assertEquals("true", result.getOptionValuesById().get("use-aviator"), 
            "Boolean flag should be set to true");
        assertEquals("myrelease", result.getOptionValuesById().get("release"), 
            "--rel should have value 'myrelease'");
    }
    
    @Test
    public void testNonBooleanOptionFollowedByOptionWithEquals() {
        List<IOptionDescriptor> options = List.of(
            OptionDescriptor.builder()
                .id("type")
                .optionNames(new String[] {"--type"})
                .description("Type")
                .bool(false)
                .build(),
            OptionDescriptor.builder()
                .id("release")
                .optionNames(new String[] {"--release", "--rel"})
                .description("Release name")
                .bool(false)
                .build()
        );
        
        var parser = new SimpleOptionsParser(options);
        var result = parser.parse(new String[] {"--type", "--rel=myrelease"});
        
        assertFalse(result.hasValidationErrors(), 
            "Should not have validation errors: " + result.getValidationErrors());
        assertEquals(null, result.getOptionValuesById().get("type"), 
            "Non-boolean option without value followed by another option should have null value");
        assertEquals("myrelease", result.getOptionValuesById().get("release"), 
            "--rel should be parsed as a separate option");
    }
    
    @Test
    public void testBooleanFlagAlone() {
        List<IOptionDescriptor> options = List.of(
            OptionDescriptor.builder()
                .id("use-aviator")
                .optionNames(new String[] {"--use-aviator"})
                .description("Use aviator")
                .bool(true)
                .build()
        );
        
        var parser = new SimpleOptionsParser(options);
        var result = parser.parse(new String[] {"--use-aviator"});
        
        assertFalse(result.hasValidationErrors(), 
            "Should not have validation errors: " + result.getValidationErrors());
        assertEquals("true", result.getOptionValuesById().get("use-aviator"), 
            "Boolean flag should be set to true");
    }
    
    @Test
    public void testOptionAlias() {
        List<IOptionDescriptor> options = List.of(
            OptionDescriptor.builder()
                .id("release")
                .optionNames(new String[] {"--release", "--rel"})
                .description("Release name")
                .bool(false)
                .build()
        );
        
        var parser = new SimpleOptionsParser(options);
        var result = parser.parse(new String[] {"--rel=myrelease"});
        
        assertFalse(result.hasValidationErrors(), 
            "Should not have validation errors: " + result.getValidationErrors());
        assertEquals("myrelease", result.getOptionValuesById().get("release"), 
            "Should accept --rel alias");
    }
}
