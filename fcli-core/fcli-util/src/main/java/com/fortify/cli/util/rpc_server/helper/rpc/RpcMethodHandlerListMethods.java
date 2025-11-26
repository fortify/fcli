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
package com.fortify.cli.util.rpc_server.helper.rpc;

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
public final class RpcMethodHandlerListMethods implements IRpcMethodHandler {
    private final ObjectMapper objectMapper;
    private final Map<String, IRpcMethodHandler> methodHandlers;
    
    private static final Map<String, String> METHOD_DESCRIPTIONS = Map.of(
        "fcli.execute", "Execute an fcli command synchronously and return structured results or stdout",
        "fcli.executeAsync", "Start async fcli command execution, returns cacheKey for retrieving results",
        "fcli.getPage", "Retrieve a page of results from cache by cacheKey (from fcli.executeAsync)",
        "fcli.cancelCollection", "Cancel an in-progress async collection by cacheKey",
        "fcli.clearCache", "Clear cache entries (specific cacheKey or all)",
        "fcli.listCommands", "List available fcli commands with optional filtering",
        "fcli.version", "Get fcli version information",
        "rpc.listMethods", "List available RPC methods"
    );
    
    @Override
    public JsonNode execute(JsonNode params) throws RpcMethodException {
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
