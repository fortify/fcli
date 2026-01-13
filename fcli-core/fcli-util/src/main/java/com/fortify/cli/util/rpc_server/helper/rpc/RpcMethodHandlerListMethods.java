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
public final class RpcMethodHandlerListMethods implements IRpcMethodHandler {
    private final ObjectMapper objectMapper;
    private final Map<String, IRpcMethodHandler> methodHandlers;
    
    private static final Map<String, String> METHOD_DESCRIPTIONS = new HashMap<>();
    
    static {
        // Core execution methods
        METHOD_DESCRIPTIONS.put("fcli.execute", "Execute an fcli command synchronously and return all results");
        METHOD_DESCRIPTIONS.put("fcli.executeAsync", "Start async fcli command execution, returns cacheKey for paged retrieval");
        METHOD_DESCRIPTIONS.put("fcli.getPage", "Retrieve a page of results from cache by cacheKey");
        METHOD_DESCRIPTIONS.put("fcli.cancelCollection", "Cancel an in-progress async collection by cacheKey");
        METHOD_DESCRIPTIONS.put("fcli.clearCache", "Clear cache entries (specific cacheKey or all)");
        
        // Info methods
        METHOD_DESCRIPTIONS.put("fcli.listCommands", "List available fcli commands with optional filtering");
        METHOD_DESCRIPTIONS.put("fcli.version", "Get fcli version information");
        METHOD_DESCRIPTIONS.put("rpc.listMethods", "List available RPC methods");
        
        // SSC session methods
        METHOD_DESCRIPTIONS.put("fcli.ssc.login", "Login to SSC (params: url, user+password or token or ci-token)");
        METHOD_DESCRIPTIONS.put("fcli.ssc.logout", "Logout from SSC session");
        
        // FoD session methods
        METHOD_DESCRIPTIONS.put("fcli.fod.login", "Login to FoD (params: url, client-id+client-secret or user+password+tenant)");
        METHOD_DESCRIPTIONS.put("fcli.fod.logout", "Logout from FoD session");
    }
    
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
