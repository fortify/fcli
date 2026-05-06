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
package com.fortify.cli.util.rpc_server.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link RPCServer}. Tests the JSON-RPC 2.0 protocol handling
 * including request parsing, response generation, and error handling.
 *
 * @author Ruud Senden
 */
class JRPCServerTest {

    private RPCServer server;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        server = new RPCServer(RPCMethodHandlerRegistry.builder().build());
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
        String response = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.buildInfo\"}");

        // Assert - notifications should not return a response
        assertNull(response);
    }

    @Test
    void shouldExecuteFcliBuildInfoMethod() throws Exception {
        // Act
        String response = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.buildInfo\",\"id\":42}");

        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertEquals("2.0", node.get("jsonrpc").asText());
        assertNotNull(node.get("result"));
        assertNull(node.get("error"));
        assertEquals(42, node.get("id").asInt());

        var result = node.get("result");
        assertTrue(result.has("version"));
        assertTrue(result.has("buildDate"));
        assertTrue(result.has("actionSchemaVersion"));
        assertTrue(result.has("projectName"));
        assertTrue(result.has("docBaseUrl"));
        assertTrue(result.has("sourceCodeBaseUrl"));
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

        var result = node.get("result");
        assertTrue(result.has("methods"));
        assertTrue(result.get("methods").isArray());
        assertTrue(result.get("methods").size() >= 4);
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
    void shouldReturnInvalidParamsForZeroLimit() throws Exception {
        // Test limit validation in job.getPage
        String response = server.processRequest(
            "{\"jsonrpc\":\"2.0\",\"method\":\"job.getPage\",\"params\":{\"jobId\":\"test-key\",\"limit\":0},\"id\":1}");

        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertNotNull(node.get("error"));
        assertEquals(-32602, node.get("error").get("code").asInt());
        assertTrue(node.get("error").get("message").asText().contains("limit"));
    }

    @Test
    void shouldReturnInvalidParamsForNegativeOffset() throws Exception {
        // Test offset validation in job.getPage
        String response = server.processRequest(
            "{\"jsonrpc\":\"2.0\",\"method\":\"job.getPage\",\"params\":{\"jobId\":\"test-key\",\"offset\":-5},\"id\":1}");

        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertNotNull(node.get("error"));
        assertEquals(-32602, node.get("error").get("code").asInt());
        assertTrue(node.get("error").get("message").asText().contains("offset"));
    }

    @Test
    void shouldPreserveRequestIdInResponse() throws Exception {
        // Test with string id
        String response1 = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.buildInfo\",\"id\":\"string-id\"}");
        assertNotNull(response1);
        var node1 = objectMapper.readTree(response1);
        assertEquals("string-id", node1.get("id").asText());

        // Test with numeric id
        String response2 = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.buildInfo\",\"id\":999}");
        assertNotNull(response2);
        var node2 = objectMapper.readTree(response2);
        assertEquals(999, node2.get("id").asInt());
    }

    @Test
    void shouldHandleBatchRequest() throws Exception {
        // Act
        String response = server.processRequest(
            "[{\"jsonrpc\":\"2.0\",\"method\":\"fcli.buildInfo\",\"id\":1}," +
            "{\"jsonrpc\":\"2.0\",\"method\":\"rpc.listMethods\",\"id\":2}]"
        );

        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertTrue(node.isArray());
        assertEquals(2, node.size());

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
        String response = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.buildInfo\",\"id\":null}");

        // Assert - no response for notifications
        assertNull(response);
    }

    @Test
    void shouldHandleRequestWithNullParams() throws Exception {
        // Act
        String response = server.processRequest("{\"jsonrpc\":\"2.0\",\"method\":\"fcli.buildInfo\",\"params\":null,\"id\":1}");

        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertNotNull(node.get("result"));
        assertNull(node.get("error"));
    }

    @Test
    void shouldExecuteFcliListCommandsWithNoQuery() throws Exception {
        String response = server.processRequest(
            "{\"jsonrpc\":\"2.0\",\"method\":\"fcli.listCommands\",\"params\":{},\"id\":1}"
        );

        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertEquals("2.0", node.get("jsonrpc").asText());
        // Result or error is acceptable here (no app context in unit tests),
        // but the response must be well-formed JSON-RPC.
        assertTrue(node.has("error") || node.has("result"));
    }

    @Test
    void shouldReturnErrorForListCommandsWithInvalidSpelQuery() throws Exception {
        String response = server.processRequest(
            "{\"jsonrpc\":\"2.0\",\"method\":\"fcli.listCommands\",\"params\":{\"query\":\"!!!invalid!!!\"},\"id\":1}"
        );

        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertEquals("2.0", node.get("jsonrpc").asText());
        assertNotNull(node.get("error"));
        assertEquals(-32603, node.get("error").get("code").asInt());
    }

    @Test
    void shouldReturnNotFoundForGetPageWithInvalidJobId() throws Exception {
        // Act
        String response = server.processRequest(
            "{\"jsonrpc\":\"2.0\",\"method\":\"job.getPage\",\"params\":{\"jobId\":\"non-existent-key\"},\"id\":1}"
        );

        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertNotNull(node.get("result"));
        assertEquals("not_found", node.get("result").get("status").asText());
    }

    @Test
    void shouldReturnInvalidParamsForGetPageWithoutJobId() throws Exception {
        // Act
        String response = server.processRequest(
            "{\"jsonrpc\":\"2.0\",\"method\":\"job.getPage\",\"params\":{},\"id\":1}"
        );

        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertNotNull(node.get("error"));
        assertEquals(-32602, node.get("error").get("code").asInt());
    }

    @Test
    void shouldHandleCancelForNonExistentKey() throws Exception {
        // Act
        String response = server.processRequest(
            "{\"jsonrpc\":\"2.0\",\"method\":\"job.cancel\",\"params\":{\"jobId\":\"non-existent-key\"},\"id\":1}"
        );

        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertNotNull(node.get("result"));
        assertEquals(false, node.get("result").get("success").asBoolean());
    }

    @Test
    void shouldReturnEmptyJobList() throws Exception {
        // Act
        String response = server.processRequest(
            "{\"jsonrpc\":\"2.0\",\"method\":\"job.list\",\"params\":{},\"id\":1}"
        );

        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertNotNull(node.get("result"));
        assertTrue(node.get("result").has("jobs"));
        assertEquals(0, node.get("result").get("totalJobs").asInt());
    }

    @Test
    void shouldListAllMethodsInRpcListMethods() throws Exception {
        // Act
        String response = server.processRequest(
            "{\"jsonrpc\":\"2.0\",\"method\":\"rpc.listMethods\",\"id\":1}"
        );

        // Assert
        assertNotNull(response);
        var node = objectMapper.readTree(response);
        assertNotNull(node.get("result"));

        var methods = node.get("result").get("methods");
        assertTrue(methods.isArray());
        assertTrue(methods.size() >= 10, "Should have at least 10 methods including job ones");

        boolean hasGetPage = false;
        boolean hasCancel = false;
        boolean hasJobList = false;

        for (var method : methods) {
            String name = method.get("name").asText();
            if ("job.getPage".equals(name)) hasGetPage = true;
            if ("job.cancel".equals(name)) hasCancel = true;
            if ("job.list".equals(name)) hasJobList = true;
        }

        assertTrue(hasGetPage, "job.getPage method should be present");
        assertTrue(hasCancel, "job.cancel method should be present");
        assertTrue(hasJobList, "job.list method should be present");
    }

    @Nested
    class CustomMethodRegistration {
        @Test
        void shouldExecuteRegisteredCustomMethod() throws Exception {
            IRPCMethodHandler handler = params -> objectMapper.createObjectNode().put("echo", "hello");
            var customServer = new RPCServer(
                RPCMethodHandlerRegistry.builder().register("custom.echo", handler).build());

            String response = customServer.processRequest(
                "{\"jsonrpc\":\"2.0\",\"method\":\"custom.echo\",\"id\":1}");
            assertNotNull(response);
            var node = objectMapper.readTree(response);
            assertNotNull(node.get("result"));
            assertEquals("hello", node.get("result").get("echo").asText());
        }

        @Test
        void shouldListCustomMethodInRpcListMethods() throws Exception {
            IRPCMethodHandler handler = params -> objectMapper.createObjectNode();
            var customServer = new RPCServer(
                RPCMethodHandlerRegistry.builder().register("fn.myFunc", handler).build());

            String response = customServer.processRequest(
                "{\"jsonrpc\":\"2.0\",\"method\":\"rpc.listMethods\",\"id\":1}");
            var node = objectMapper.readTree(response);
            var methods = node.get("result").get("methods");

            Set<String> methodNames = new HashSet<>();
            for (var method : methods) {
                methodNames.add(method.get("name").asText());
            }
            assertTrue(methodNames.contains("fn.myFunc"), "Custom method should be listed");
        }

        @Test
        void shouldSupportCustomMethodRegistration() throws Exception {
            IRPCMethodHandler handler = params -> {
                var result = objectMapper.createObjectNode();
                result.put("value", params != null && params.has("x") ? params.get("x").asInt() * 2 : 0);
                return result;
            };
            var noDefaultServer = new RPCServer(
                RPCMethodHandlerRegistry.builder().register("fn.double", handler).build());

            String response = noDefaultServer.processRequest(
                "{\"jsonrpc\":\"2.0\",\"method\":\"fn.double\",\"params\":{\"x\":21},\"id\":1}");
            assertNotNull(response);
            var node = objectMapper.readTree(response);
            assertNotNull(node.get("result"));
            assertEquals(42, node.get("result").get("value").asInt());
        }
    }

    @Nested
    class JobGetStatusMethod {
        @Test
        void shouldReturnNotFoundForUnknownJobId() throws Exception {
            String response = server.processRequest(
                "{\"jsonrpc\":\"2.0\",\"method\":\"job.getStatus\",\"params\":{\"jobId\":\"unknown\"},\"id\":1}");

            assertNotNull(response);
            var node = objectMapper.readTree(response);
            assertNotNull(node.get("result"));
            assertEquals("not_found", node.get("result").get("status").asText());
            assertEquals("unknown", node.get("result").get("jobId").asText());
        }

        @Test
        void shouldReturnInvalidParamsWithoutJobId() throws Exception {
            String response = server.processRequest(
                "{\"jsonrpc\":\"2.0\",\"method\":\"job.getStatus\",\"params\":{},\"id\":1}");

            assertNotNull(response);
            var node = objectMapper.readTree(response);
            assertNotNull(node.get("error"));
            assertEquals(-32602, node.get("error").get("code").asInt());
        }
    }

    @Nested
    class JobRemoveMethod {
        @Test
        void shouldReturnNotFoundForUnknownJob() throws Exception {
            String response = server.processRequest(
                "{\"jsonrpc\":\"2.0\",\"method\":\"job.remove\",\"params\":{\"jobId\":\"unknown\"},\"id\":1}");

            assertNotNull(response);
            var node = objectMapper.readTree(response);
            assertNotNull(node.get("result"));
            assertFalse(node.get("result").get("success").asBoolean());
        }

        @Test
        void shouldReturnInvalidParamsWithoutJobId() throws Exception {
            String response = server.processRequest(
                "{\"jsonrpc\":\"2.0\",\"method\":\"job.remove\",\"params\":{},\"id\":1}");

            assertNotNull(response);
            var node = objectMapper.readTree(response);
            assertNotNull(node.get("error"));
            assertEquals(-32602, node.get("error").get("code").asInt());
        }
    }

    @Nested
    class MethodDiscovery {
        @Test
        void shouldListNewMethodsInRpcListMethods() throws Exception {
            String response = server.processRequest(
                "{\"jsonrpc\":\"2.0\",\"method\":\"rpc.listMethods\",\"id\":1}");

            var node = objectMapper.readTree(response);
            var methods = node.get("result").get("methods");
            Set<String> methodNames = new HashSet<>();
            for (var method : methods) {
                methodNames.add(method.get("name").asText());
            }
            assertTrue(methodNames.contains("job.getStatus"), "job.getStatus should be listed");
            assertTrue(methodNames.contains("job.remove"), "job.remove should be listed");
        }
    }

}
