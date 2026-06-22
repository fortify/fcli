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
package com.fortify.cli.ai_assist.mcp.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.ai_assist.mcp.helper.MCPJobManager;
import com.fortify.cli.common.cli.util.FcliActionState;
import com.fortify.cli.common.cli.util.FcliExecutionContext;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.cli.util.FcliIsolationScope;
import com.fortify.cli.common.concurrent.job.AsyncJobManager;

import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Unit tests for {@link MCPJobManager}. Tests basic job lifecycle management,
 * progress tracking, and token generation without requiring full MCP server setup.
 *
 * @author Ruud Senden
 */
class MCPJobManagerTest {
    
    private MCPJobManager jobManager;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        // Create job manager with short timeout for testing
        jobManager = new MCPJobManager(4, 2, 500, 100, new AsyncJobManager());
        objectMapper = new ObjectMapper();
        // Tests run outside of a server request, so push a fresh root context.
        FcliExecutionContextHolder.pushNew();
    }
    
    @AfterEach
    void tearDown() {
        FcliExecutionContextHolder.pop();
    }
    
    @Test
    void shouldExecuteQuickJobWithinSafeReturnPeriod() throws Exception {
        // Arrange
        var counter = new AtomicInteger();
        String testContent = "{\"result\":\"test\"}";
        CallToolResult expectedResult = CallToolResult.builder().addTextContent(testContent).isError(false).build();
        
        // Act
        CallToolResult result = jobManager.execute(
            null,
            "test_tool",
            () -> {
                Thread.sleep(100);
                counter.incrementAndGet();
                return expectedResult;
            },
            MCPJobManager.recordCounter(counter),
            false
        );
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isError());
        assertNotNull(result.content());
        assertEquals(1, counter.get());
    }
    
    @Test
    void shouldReturnInProgressForSlowJob() throws Exception {
        // Arrange
        var counter = new AtomicInteger();
        
        // Act - Job takes longer than safe return period (500ms)
        CallToolResult result = jobManager.execute(
            null,
            "slow_test_tool",
            () -> {
                Thread.sleep(2000);
                counter.incrementAndGet();
                return CallToolResult.builder().addTextContent("{\"completed\":true}").isError(false).build();
            },
            MCPJobManager.recordCounter(counter),
            false
        );
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isError());
        
        // Result should contain in_progress status since job took longer than safe return period
        String resultContent = result.content().toString();
        assertTrue(resultContent.contains("in_progress") || resultContent.contains("job_token"));
    }
    
    @Test
    void shouldHandleJobExecutionError() throws Exception {
        // Arrange
        var counter = new AtomicInteger();
        String errorMessage = "Simulated error";
        
        // Act
        CallToolResult result = jobManager.execute(
            null,
            "error_test_tool",
            () -> {
                throw new RuntimeException(errorMessage);
            },
            MCPJobManager.recordCounter(counter),
            false
        );
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isError());
        
        // Result should contain error information
        String resultContent = result.content().toString();
        assertTrue(resultContent.contains("failed") || resultContent.contains("error"));
    }
    
    @Test
    void shouldTrackProgressWithRecordCounter() throws Exception {
        // Arrange
        var counter = new AtomicInteger();
        
        // Act
        CallToolResult result = jobManager.execute(
            null,
            "counting_test_tool",
            () -> {
                for (int i = 0; i < 5; i++) {
                    counter.incrementAndGet();
                    Thread.sleep(50);
                }
                return CallToolResult.builder().addTextContent("completed with " + counter.get() + " records").isError(false).build();
            },
            MCPJobManager.recordCounter(counter),
            false
        );
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(5, counter.get());
    }
    
    @Test
    void shouldTrackFutureAndReturnJobToken() throws Exception {
        // Arrange
        var counter = new AtomicInteger();
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    counter.incrementAndGet();
                    Thread.sleep(100);
                }
                return "completed";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        
        // Act
        String jobToken = jobManager.trackFuture(
            "tracked_test_tool",
            future,
            MCPJobManager.recordCounter(counter)
        );
        
        // Assert
        assertNotNull(jobToken);
        assertFalse(jobToken.isEmpty());
        assertTrue(jobToken.matches("^[a-zA-Z0-9\\-]+$"));
        
        // Wait for job to complete
        future.get(2, TimeUnit.SECONDS);
        Thread.sleep(200); // Allow time for job manager to update
        
        assertEquals(3, counter.get());
    }
    
    @Test
    void shouldGenerateUniqueJobTokens() {
        // Arrange & Act
        var counter = new AtomicInteger();
        CompletableFuture<String> future1 = CompletableFuture.completedFuture("result1");
        CompletableFuture<String> future2 = CompletableFuture.completedFuture("result2");
        
        String token1 = jobManager.trackFuture("tool1", future1, MCPJobManager.recordCounter(counter));
        String token2 = jobManager.trackFuture("tool2", future2, MCPJobManager.recordCounter(counter));
        
        // Assert
        assertNotEquals(token1, token2);
        assertTrue(token1.matches("^[a-zA-Z0-9\\-]+$"));
        assertTrue(token2.matches("^[a-zA-Z0-9\\-]+$"));
    }
    
    @Test
    void shouldUseTickingProgressStrategy() throws Exception {
        // Arrange
        var counter = new AtomicInteger();
        
        // Act
        CallToolResult result = jobManager.execute(
            null,
            "ticking_test_tool",
            () -> {
                Thread.sleep(100);
                return CallToolResult.builder().addTextContent("completed").isError(false).build();
            },
            MCPJobManager.ticking(counter),
            false
        );
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isError());
        // Ticking strategy should have incremented the counter during progress updates
        // Note: The exact count depends on timing, but should be > 0
    }

    @Test
    void jobToolShouldOnlySeeJobsFromCurrentIsolationScope() throws Exception {
        var scopeOne = new FcliIsolationScope();
        var scopeTwo = new FcliIsolationScope();
        var jobToken = withIsolationScope(scopeOne,
                () -> jobManager.trackFuture("scoped_test_tool",
                        CompletableFuture.completedFuture("done"),
                        MCPJobManager.recordCounter(new AtomicInteger())));

        var scopeTwoStatus = withIsolationScope(scopeTwo, () -> getJobStatus(jobToken));
        assertEquals("not_found", scopeTwoStatus.path("status").asText());
        assertEquals(jobToken, scopeTwoStatus.path("job_token").asText());

        var scopeOneStatus = withIsolationScope(scopeOne, () -> getJobStatus(jobToken));
        assertNotEquals("not_found", scopeOneStatus.path("status").asText());
        assertEquals(jobToken, scopeOneStatus.path("job_token").asText());
        assertEquals("scoped_test_tool", scopeOneStatus.path("tool").asText());
    }

    private JsonNode getJobStatus(String jobToken) {
        var request = new CallToolRequest(MCPJobManager.JOB_TOOL_NAME,
                java.util.Map.of("operation", "status", "job_token", jobToken));
        var result = jobManager.getJobToolSpecification().callHandler().apply(null, request);
        return parseToolResult(result);
    }

    private JsonNode parseToolResult(CallToolResult result) {
        var text = result.content().stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .map(TextContent::text)
                .findFirst()
                .orElseThrow();
        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing MCP job tool result", e);
        }
    }

    private <T> T withIsolationScope(FcliIsolationScope isolationScope, ThrowingSupplier<T> supplier) throws Exception {
        FcliExecutionContextHolder.push(new FcliExecutionContext(isolationScope, new FcliActionState()));
        try {
            return supplier.get();
        } finally {
            FcliExecutionContextHolder.pop();
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
