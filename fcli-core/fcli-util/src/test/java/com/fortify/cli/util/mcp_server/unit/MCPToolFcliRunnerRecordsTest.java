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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlers;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerRecords;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

/**
 * Unit tests for {@link MCPToolFcliRunnerRecords}. Tests the basic structure
 * and configuration of the records runner.
 * 
 * Note: Full integration testing with actual command execution is done in
 * functional tests. These unit tests verify the runner's initialization and
 * basic properties.
 *
 * @author Ruud Senden
 */
class MCPToolFcliRunnerRecordsTest {
    
    private MCPJobManager jobManager;
    private MCPToolArgHandlers argHandlers;
    private CommandSpec commandSpec;
    private MCPToolFcliRunnerRecords runner;
    
    @BeforeEach
    void setUp() {
        // Create test command
        var cmd = new TestGetCommand();
        var commandLine = new CommandLine(cmd);
        commandSpec = commandLine.getCommandSpec();
        
        // Create dependencies
        jobManager = new MCPJobManager("test", 2, 1, 500, 100);
        argHandlers = new MCPToolArgHandlers(commandSpec);
        
        // Create runner
        runner = new MCPToolFcliRunnerRecords(argHandlers, commandSpec, jobManager);
    }
    
    @Test
    void shouldInitializeWithRequiredComponents() {
        // Assert
        assertNotNull(runner);
        assertNotNull(runner.getToolSpecArgHelper());
        assertNotNull(runner.getCommandSpec());
        assertEquals(argHandlers, runner.getToolSpecArgHelper());
        assertEquals(commandSpec, runner.getCommandSpec());
    }
    
    @Test
    void shouldHaveCorrectCommandSpec() {
        // Assert
        assertEquals("get-item", runner.getCommandSpec().name());
    }
    
    @Test
    void shouldUseArgHandlersForSchema() {
        // Act
        var schema = runner.getToolSpecArgHelper().getSchema();
        
        // Assert
        assertNotNull(schema);
        assertEquals("object", schema.type());
        assertFalse(schema.properties().isEmpty());
    }
    
    @Test
    void shouldUseArgHandlersForCommandArgBuilding() {
        // Arrange
        java.util.Map<String, Object> toolArgs = java.util.Map.of("id", "test-123");
        
        // Act
        String cmdArgs = runner.getToolSpecArgHelper().getFcliCmdArgs(toolArgs);
        
        // Assert
        assertNotNull(cmdArgs);
        // Just verify it generates command arguments
    }
    
    @Test
    void shouldNotBePagedForGetCommand() {
        // Assert
        assertFalse(runner.getToolSpecArgHelper().isPaged());
    }
    
    @Test
    void shouldBePagedForListCommand() {
        // Arrange
        var listCmd = new TestListCommand();
        var listCommandLine = new CommandLine(listCmd);
        var listCommandSpec = listCommandLine.getCommandSpec();
        var listArgHandlers = new MCPToolArgHandlers(listCommandSpec);
        var listRunner = new MCPToolFcliRunnerRecords(listArgHandlers, listCommandSpec, jobManager);
        
        // Assert
        assertTrue(listRunner.getToolSpecArgHelper().isPaged());
    }
    
    // Note: Testing actual command execution with CallToolRequest requires
    // a full MCP server setup and is better suited for functional tests.
    // These unit tests focus on runner initialization and configuration.
    
    // Test command classes
    
    @Command(name = "get-item")
    static class TestGetCommand {
        @Option(names = "--id", required = true)
        String id;
    }
    
    @Command(name = "list-items")
    static class TestListCommand {
        @Option(names = "--filter")
        String filter;
    }
}
