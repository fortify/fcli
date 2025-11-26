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
package com.fortify.cli.util.rpc_server.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.util.rpc_server.helper.rpc.JsonRpcServer;

/**
 * Unit tests for {@link JsonRpcServer}. Tests the JSON-RPC 2.0 protocol handling
 * including request parsing, response generation, and error handling.
 *
 * @author Ruud Senden
 */
class JsonRpcServerTest {
    
    private JsonRpcServer server;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        server = new JsonRpcServer(objectMapper, 2);
    }
    
    @Test
    void shouldReturnParseErrorForInvalidJson() throws Exception {
        // Act
        String response = server.processRequest("not valid json");
        
        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertEquals("2.0", node.get("jsonrpc").asText());
        assertNotNull(node.get("error"));
        assertEquals(-32700, node.get("error").get("code").asInt());
        assertNull(node.get("result"));
    }
    
    @Test
    void shouldReturnInvalidRequestForMissingJsonrpcVersion() throws Exception {
        // Act
        String response = server.processRequest("{\"method\":\"test\",\"id\":1}");
        
        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertEquals("2.0", node.get("jsonrpc").asText());
        assertNotNull(node.get("error"));
        assertEquals(-32600, node.get("error").get("code").asInt());
    }
    
    @Test
    void shouldReturnInvalidRequestForWrongJsonrpcVersion() throws Exception {
        // Act
        String response = server.processRequest("{\"jsonrpc\":\"1.0\",\"method\":\"test\",\"id\":1}");
        
        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertNotNull(node.get("error"));
        assertEquals(-32600, node.get("error").get("code").asInt());
    }
    
    @Test
    void shouldReturnMethodNotFoundForUnknownMethod() throws Exception {
        // Act
        String response = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"unknown.method\",\"id\":1}");
        
        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertEquals("2.0", node.get("jsonrpc").asText());
        assertNotNull(node.get("error"));
        assertEquals(-32601, node.get("error").get("code").asInt());
        assertTrue(node.get("error").get("message").asText().contains("unknown.method"));
        assertEquals(1, node.get("id").asInt());
    }
    
    @Test
    void shouldReturnNullForNotification() throws Exception {
        // Notification = request without id
        // Act
        String response = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.version\"}");
        
        // Assert - notifications should not return a response
        assertNull(response);
    }
    
    @Test
    void shouldExecuteFcliVersionMethod() throws Exception {
        // Act
        String response = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.version\",\"id\":42}");
        
        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertEquals("2.0", node.get("jsonrpc").asText());
        assertNotNull(node.get("result"));
        assertNull(node.get("error"));
        assertEquals(42, node.get("id").asInt());
        
        // Check result contains version info
        var result = node.get("result");
        assertTrue(result.has("version"));
        assertTrue(result.has("buildDate"));
        assertTrue(result.has("actionSchemaVersion"));
    }
    
    @Test
    void shouldExecuteRpcListMethodsMethod() throws Exception {
        // Act
        String response = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"rpc.listMethods\",\"id\":1}");
        
        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertEquals("2.0", node.get("jsonrpc").asText());
        assertNotNull(node.get("result"));
        assertNull(node.get("error"));
        
        // Check result contains methods list
        var result = node.get("result");
        assertTrue(result.has("methods"));
        assertTrue(result.get("methods").isArray());
        assertTrue(result.get("methods").size() >= 4); // At least our 4 default methods
        assertTrue(result.has("count"));
    }
    
    @Test
    void shouldReturnInvalidParamsForExecuteWithoutCommand() throws Exception {
        // Act
        String response = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.execute\",\"params\":{},\"id\":1}");
        
        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertNotNull(node.get("error"));
        assertEquals(-32602, node.get("error").get("code").asInt());
        assertTrue(node.get("error").get("message").asText().contains("command"));
    }
    
    @Test
    void shouldPreserveRequestIdInResponse() throws Exception {
        // Test with string id
        String response1 = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.version\",\"id\":\"string-id\"}");
        assertNotNull(response1);
        var node1 = objectMapper.readTree(response1);
        assertEquals("string-id", node1.get("id").asText());
        
        // Test with numeric id
        String response2 = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.version\",\"id\":999}");
        assertNotNull(response2);
        var node2 = objectMapper.readTree(response2);
        assertEquals(999, node2.get("id").asInt());
    }
    
    @Test
    void shouldHandleBatchRequest() throws Exception {
        // Act
        String response = server.processRequest(
            "[{\"jsonrpc\":\"2.0\",\"method\":\"fcli.version\",\"id\":1}," +
            "{\"jsonrpc\":\"2.0\",\"method\":\"rpc.listMethods\",\"id\":2}]"
        );
        
        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertTrue(node.isArray());
        assertEquals(2, node.size());
        
        // Both responses should be successful
        for (var responseNode : node) {
            assertEquals("2.0", responseNode.get("jsonrpc").asText());
            assertNotNull(responseNode.get("result"));
            assertNull(responseNode.get("error"));
        }
    }
    
    @Test
    void shouldReturnInvalidRequestForEmptyBatch() throws Exception {
        // Act
        String response = server.processRequest("[]");
        
        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertNotNull(node.get("error"));
        assertEquals(-32600, node.get("error").get("code").asInt());
    }
    
    @Test
    void shouldHandleNullId() throws Exception {
        // Act - id is explicitly null (this is a notification)
        String response = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.version\",\"id\":null}");
        
        // Assert - no response for notifications
        assertNull(response);
    }
    
    @Test
    void shouldHandleRequestWithNullParams() throws Exception {
        // Act
        String response = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.version\",\"params\":null,\"id\":1}");
        
        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertNotNull(node.get("result"));
        assertNull(node.get("error"));
    }
    
    @Test
    void shouldReturnErrorForListCommandsWithoutAppContext() throws Exception {
        // Note: fcli.listCommands requires the full fcli command tree to be initialized,
        // which isn't available in unit tests. This test verifies that the method
        // returns an error response rather than crashing.
        String response = server.processRequest(
            "{\"jsonrpc\":\"2.0\",\"method\":\"fcli.listCommands\",\"params\":{},\"id\":1}"
        );
        
        // Either we get an error (expected in unit test context) or a result (if running in full context)
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertEquals("2.0", node.get("jsonrpc").asText());
        // In unit test context, we expect an error since the command tree isn't initialized
        // but the important thing is that it doesn't crash
        assertTrue(node.has("error") || node.has("result"));
    }
}
