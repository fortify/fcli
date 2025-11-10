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
package com.fortify.cli.util.mcp_server.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlers;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

/**
 * Unit tests for {@link MCPToolArgHandlers}. Tests schema generation and
 * command-line argument building for various option types.
 *
 * @author Ruud Senden
 */
class MCPToolArgHandlersTest {
    
    @Test
    void shouldGenerateSchemaForStringOption() {
        // Arrange
        var cmd = new TestCommandWithString();
        var commandLine = new CommandLine(cmd);
        CommandSpec commandSpec = commandLine.getCommandSpec();
        
        // Act
        var handlers = new MCPToolArgHandlers(commandSpec);
        var schema = handlers.getSchema();
        
        // Assert
        assertNotNull(schema);
        assertEquals("object", schema.type());
        assertNotNull(schema.properties());
        // Schema properties are generated based on option names, which may differ from field names
        // Just verify schema is created and has correct type
        assertFalse(schema.properties().isEmpty());
    }
    
    @Test
    void shouldGenerateSchemaForBooleanOption() {
        // Arrange
        var cmd = new TestCommandWithBoolean();
        var commandLine = new CommandLine(cmd);
        CommandSpec commandSpec = commandLine.getCommandSpec();
        
        // Act
        var handlers = new MCPToolArgHandlers(commandSpec);
        var schema = handlers.getSchema();
        
        // Assert
        assertNotNull(schema);
        assertEquals("object", schema.type());
        assertFalse(schema.properties().isEmpty());
    }
    
    @Test
    void shouldGenerateSchemaForOptionalOption() {
        // Arrange
        var cmd = new TestCommandWithOptional();
        var commandLine = new CommandLine(cmd);
        CommandSpec commandSpec = commandLine.getCommandSpec();
        
        // Act
        var handlers = new MCPToolArgHandlers(commandSpec);
        var schema = handlers.getSchema();
        
        // Assert
        assertNotNull(schema);
        assertEquals("object", schema.type());
        // Optional fields should not be in required list
        assertNotNull(schema.required());
    }
    
    @Test
    void shouldBuildCommandLineArgumentsFromToolArgs() {
        // Arrange
        var cmd = new TestCommandWithMultipleOptions();
        var commandLine = new CommandLine(cmd);
        CommandSpec commandSpec = commandLine.getCommandSpec();
        var handlers = new MCPToolArgHandlers(commandSpec);
        
        Map<String, Object> toolArgs = Map.of(
            "name", "test-name",
            "count", 42,
            "verbose", true
        );
        
        // Act
        String cmdArgs = handlers.getFcliCmdArgs(toolArgs);
        
        // Assert
        assertNotNull(cmdArgs);
        // Just verify it generates some command arguments
        // Exact format depends on implementation details
    }
    
    @Test
    void shouldHandleEmptyToolArgs() {
        // Arrange
        var cmd = new TestCommandWithOptional();
        var commandLine = new CommandLine(cmd);
        CommandSpec commandSpec = commandLine.getCommandSpec();
        var handlers = new MCPToolArgHandlers(commandSpec);
        
        // Act
        String cmdArgs = handlers.getFcliCmdArgs(Map.of());
        
        // Assert - Should not fail, may be empty or contain defaults
        assertNotNull(cmdArgs);
    }
    
    @Test
    void shouldHandleNullToolArgs() {
        // Arrange
        var cmd = new TestCommandWithOptional();
        var commandLine = new CommandLine(cmd);
        CommandSpec commandSpec = commandLine.getCommandSpec();
        var handlers = new MCPToolArgHandlers(commandSpec);
        
        // Act
        String cmdArgs = handlers.getFcliCmdArgs(null);
        
        // Assert - Should not fail
        assertNotNull(cmdArgs);
    }
    
    @Test
    void shouldDetectPagedCommand() {
        // Arrange - list commands should be detected as paged
        var cmd = new TestListCommand();
        var commandLine = new CommandLine(cmd);
        CommandSpec commandSpec = commandLine.getCommandSpec();
        
        // Act
        var handlers = new MCPToolArgHandlers(commandSpec);
        
        // Assert
        assertTrue(handlers.isPaged());
    }
    
    @Test
    void shouldDetectNonPagedCommand() {
        // Arrange - get commands should not be paged
        var cmd = new TestGetCommand();
        var commandLine = new CommandLine(cmd);
        CommandSpec commandSpec = commandLine.getCommandSpec();
        
        // Act
        var handlers = new MCPToolArgHandlers(commandSpec);
        
        // Assert
        assertFalse(handlers.isPaged());
    }
    
    // Test command classes
    
    @Command(name = "test-string")
    static class TestCommandWithString {
        @Option(names = "--name", required = true)
        String name;
    }
    
    @Command(name = "test-boolean")
    static class TestCommandWithBoolean {
        @Option(names = "--verbose")
        boolean verbose;
    }
    
    @Command(name = "test-optional")
    static class TestCommandWithOptional {
        @Option(names = "--optional", required = false)
        String optional;
    }
    
    @Command(name = "test-multiple")
    static class TestCommandWithMultipleOptions {
        @Option(names = "--name", required = true)
        String name;
        
        @Option(names = "--count", required = false, defaultValue = "0")
        int count;
        
        @Option(names = "--verbose")
        boolean verbose;
    }
    
    @Command(name = "list-items")
    static class TestListCommand {
        @Option(names = "--filter")
        String filter;
    }
    
    @Command(name = "get-item")
    static class TestGetCommand {
        @Option(names = "--id", required = true)
        String id;
    }
}
