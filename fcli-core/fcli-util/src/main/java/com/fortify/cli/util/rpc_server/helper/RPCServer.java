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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.util._common.helper.FcliRecordsCache;

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
 * - Includes caching for efficient paged access to large result sets
 * 
 * @author Ruud Senden
 */
@Slf4j
public final class RPCServer {
    private final ObjectMapper objectMapper;
    private final Map<String, IRPCMethodHandler> methodHandlers;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final FcliRecordsCache cache;
    
    public RPCServer(ObjectMapper objectMapper) {
        this(objectMapper, true);
    }

    public RPCServer(ObjectMapper objectMapper, boolean registerDefaults) {
        this.objectMapper = objectMapper;
        this.methodHandlers = new LinkedHashMap<>();
        this.cache = new FcliRecordsCache();
        if (registerDefaults) {
            registerDefaultFcliMethods();
        }
        registerMethod("rpc.listMethods", new RPCMethodHandlerListMethods(objectMapper, methodHandlers));
    }
    
    private void registerDefaultFcliMethods() {
        registerMethod("fcli.execute", new RPCMethodHandlerFcliExecute(objectMapper));
        registerMethod("fcli.executeAsync", new RPCMethodHandlerFcliExecuteAsync(objectMapper, cache));
        registerMethod("fcli.getPage", new RPCMethodHandlerFcliGetPage(objectMapper, cache));
        registerMethod("fcli.cancelCollection", new RPCMethodHandlerFcliCancelCollection(objectMapper, cache));
        registerMethod("fcli.clearCache", new RPCMethodHandlerFcliClearCache(objectMapper, cache));
        registerMethod("fcli.listCommands", new RPCMethodHandlerFcliListCommands(objectMapper));
        registerMethod("fcli.version", new RPCMethodHandlerFcliVersion(objectMapper));
    }
    
    /**
     * Register a custom method handler.
     */
    public void registerMethod(String methodName, IRPCMethodHandler handler) {
        methodHandlers.put(methodName, handler);
        log.debug("Registered RPC method: {}", methodName);
    }
    
    /**
     * Start the server, reading from the given input stream and writing to the output stream.
     * This method blocks until the input stream is closed or an error occurs.
     * Requests are processed synchronously in the order they are received.
     */
    public void start(InputStream input, OutputStream output) {
        running.set(true);
        log.info("JSON-RPC server starting on stdio");
        System.err.println("Fcli JSON-RPC server running on stdio. Hit Ctrl-C to exit.");
        
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
            cache.shutdown();
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
            JsonNode requestNode = objectMapper.readTree(requestJson);
            
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
        
        ArrayNode responses = objectMapper.createArrayNode();
        for (JsonNode request : requests) {
            String responseJson = processSingleRequest(request);
            if (responseJson != null) {
                try {
                    responses.add(objectMapper.readTree(responseJson));
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
            request = objectMapper.treeToValue(requestNode, RPCRequest.class);
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
        var handler = methodHandlers.get(request.method());
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
            return objectMapper.writeValueAsString(obj);
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
