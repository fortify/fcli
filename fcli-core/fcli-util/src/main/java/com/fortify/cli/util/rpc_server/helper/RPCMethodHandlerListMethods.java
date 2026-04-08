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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/**
 * RPC method handler for listing available RPC methods.
 * 
 * Method: rpc.listMethods
 * Params: none
 * 
 * Returns:
 *   - methods (array): Array of method descriptors with:
 *     - name (string): Method name
 *     - description (string): Method description
 *
 * @author Ruud Senden
 */
@RequiredArgsConstructor
public final class RPCMethodHandlerListMethods implements IRPCMethodHandler {
    private final ObjectMapper objectMapper;
    private final Map<String, IRPCMethodHandler> methodHandlers;
    
    private static final Map<String, String> METHOD_DESCRIPTIONS = new HashMap<>();
    
    static {
        // Core execution methods
        METHOD_DESCRIPTIONS.put("fcli.execute", "Execute an fcli command synchronously and return all results");
        METHOD_DESCRIPTIONS.put("fcli.executeAsync", "Start async fcli command execution; returns cacheKey for paged retrieval via rpc.getPage");
        // Info methods
        METHOD_DESCRIPTIONS.put("fcli.listCommands", "List available fcli commands with optional filtering");
        METHOD_DESCRIPTIONS.put("fcli.version", "Get fcli version information");
        // RPC protocol methods — always registered
        METHOD_DESCRIPTIONS.put("rpc.listMethods", "List available RPC methods");
        METHOD_DESCRIPTIONS.put("rpc.getPage", "Retrieve a page of results from cache by cacheKey (works for both fcli.executeAsync and streaming fn.* calls)");
        METHOD_DESCRIPTIONS.put("rpc.cancelCollection", "Cancel an in-progress background collection by cacheKey");
        METHOD_DESCRIPTIONS.put("rpc.clearCache", "Clear cache entries (specific cacheKey or all)");
    }
    
    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        ArrayNode methods = objectMapper.createArrayNode();
        
        for (String methodName : methodHandlers.keySet()) {
            ObjectNode method = objectMapper.createObjectNode();
            method.put("name", methodName);
            method.put("description", METHOD_DESCRIPTIONS.getOrDefault(methodName, "No description available"));
            methods.add(method);
        }
        
        ObjectNode result = objectMapper.createObjectNode();
        result.set("methods", methods);
        result.put("count", methods.size());
        
        return result;
    }
}
