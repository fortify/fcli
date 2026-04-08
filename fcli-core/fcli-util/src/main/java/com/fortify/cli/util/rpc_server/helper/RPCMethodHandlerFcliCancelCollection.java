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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.util._common.helper.FcliRecordsCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for cancelling an in-progress collection.
 * 
 * Method: rpc.cancelCollection
 * Params:
 *   - cacheKey (string, required): Cache key from fcli.executeAsync
 * 
 * Returns:
 *   - success (boolean): Whether cancellation was successful
 *   - message (string): Human-readable status message
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerFcliCancelCollection implements IRPCMethodHandler {
    private final ObjectMapper objectMapper;
    private final FcliRecordsCache cache;
    
    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("cacheKey")) {
            throw RPCMethodException.invalidParams("'cacheKey' parameter is required");
        }
        
        var cacheKey = params.get("cacheKey").asText();
        if (cacheKey == null || cacheKey.isBlank()) {
            throw RPCMethodException.invalidParams("'cacheKey' cannot be empty");
        }
        
        log.debug("Cancelling collection: cacheKey={}", cacheKey);
        
        var cancelled = cache.cancel(cacheKey);
        
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", cancelled);
        result.put("cacheKey", cacheKey);
        result.put("message", cancelled 
            ? "Collection cancelled successfully" 
            : "No in-progress collection found for this cacheKey");
        
        return result;
    }
}
