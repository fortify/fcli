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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * A lightweight JSON-RPC 2.0 server that reads requests from an input stream
 * and writes responses to an output stream (typically stdin/stdout for IDE integration).
 *
 * This implementation:
 * - Supports JSON-RPC 2.0 specification
 * - Handles single requests and batch requests
 * - Supports notifications (requests without id)
 * - Is compatible with GraalVM native image compilation
 * - Processes requests synchronously (appropriate for stdio-based IDE integration)
 *
 * Method handlers and the shared cache are managed by {@link RPCMethodHandlerRegistry},
 * which is built via {@link RPCMethodHandlerRegistry#builder()} and passed to the
 * constructor.
 *
 * @author Ruud Senden
 */
@Slf4j
public final class RPCServer {
    private static final ObjectMapper OM = JsonHelper.getObjectMapper();
    private final RPCMethodHandlerRegistry registry;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public RPCServer(RPCMethodHandlerRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * Start the server, reading from the given input stream and writing to the output stream.
     * Status messages (like the startup message) are written to {@code System.err}.
     * This method blocks until the input stream is closed or an error occurs.
     * Requests are processed synchronously in the order they are received.
     */
    public void start(InputStream input, OutputStream output) {
        start(input, output, System.err);
    }
    
    /**
     * Start the server, reading from the given input stream and writing to the output stream.
     * Status messages (like the startup message) are written to {@code statusOutput}.
     * This method blocks until the input stream is closed or an error occurs.
     * Requests are processed synchronously in the order they are received.
     */
    public void start(InputStream input, OutputStream output, OutputStream statusOutput) {
        running.set(true);
        log.info("JSON-RPC server starting on stdio");
        var statusWriter = new PrintWriter(statusOutput, true, StandardCharsets.UTF_8);
        statusWriter.println("Fcli JSON-RPC server running on stdio. Hit Ctrl-C to exit.");
        statusWriter.flush();
        
        try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
             var writer = new PrintWriter(output, true, StandardCharsets.UTF_8)) {
            
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                
                log.debug("Received request: {}", line);
                
                String responseJson = processRequest(line);
                if (responseJson != null) {
                    log.debug("Sending response: {}", responseJson);
                    writer.println(responseJson);
                }
            }
        } catch (Exception e) {
            log.error("Error in JSON-RPC server", e);
        } finally {
            running.set(false);
            registry.getCache().shutdown();
            log.info("JSON-RPC server stopped");
        }
    }
    
    /**
     * Stop the server gracefully.
     */
    public void stop() {
        running.set(false);
    }
    
    /**
     * Process a single JSON-RPC request line and return the response JSON.
     * Returns null for notifications (requests without id).
     */
    public String processRequest(String requestJson) {
        try {
            JsonNode requestNode = OM.readTree(requestJson);
            
            // Check for batch request
            if (requestNode.isArray()) {
                return processBatchRequest((ArrayNode) requestNode);
            }
            
            // Single request
            return processSingleRequest(requestNode);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON-RPC request: {}", e.getMessage());
            return toJson(RPCResponse.parseError());
        }
    }
    
    private String processBatchRequest(ArrayNode requests) {
        if (requests.isEmpty()) {
            return toJson(RPCResponse.invalidRequest(null));
        }
        
        ArrayNode responses = OM.createArrayNode();
        for (JsonNode request : requests) {
            String responseJson = processSingleRequest(request);
            if (responseJson != null) {
                try {
                    responses.add(OM.readTree(responseJson));
                } catch (JsonProcessingException e) {
                    log.error("Error processing batch response", e);
                }
            }
        }
        
        // If all requests were notifications, return nothing
        if (responses.isEmpty()) {
            return null;
        }
        
        return toJson(responses);
    }
    
    private String processSingleRequest(JsonNode requestNode) {
        RPCRequest request;
        try {
            request = OM.treeToValue(requestNode, RPCRequest.class);
        } catch (JsonProcessingException e) {
            return toJson(RPCResponse.invalidRequest(null));
        }
        
        if (request == null || !request.isValid()) {
            return toJson(RPCResponse.invalidRequest(request != null ? request.id() : null));
        }
        
        // Process the method
        RPCResponse response = executeMethod(request);
        
        // Don't return response for notifications
        if (request.isNotification()) {
            return null;
        }
        
        return toJson(response);
    }
    
    private RPCResponse executeMethod(RPCRequest request) {
        var handler = registry.get(request.method());
        if (handler == null) {
            return RPCResponse.methodNotFound(request.id(), request.method());
        }
        
        try {
            JsonNode result = handler.execute(request.params());
            return RPCResponse.success(request.id(), result);
        } catch (RPCMethodException e) {
            return RPCResponse.error(request.id(), e.toJsonRpcError());
        } catch (Exception e) {
            log.error("Unexpected error executing method {}: {}", request.method(), e.getMessage(), e);
            return RPCResponse.internalError(request.id(), e.getMessage());
        }
    }
    
    private String toJson(Object obj) {
        try {
            return OM.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response", e);
            // Fallback to a hardcoded error response to avoid infinite recursion
            // if serialization itself fails
            return String.format(
                "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":%d,\"message\":\"Internal error: serialization failed\"},\"id\":null}",
                RPCError.INTERNAL_ERROR);
        }
    }
}
