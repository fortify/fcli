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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.util._common.helper.FcliRecordsCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for clearing cache entries.
 * 
 * Method: fcli.clearCache
 * Params:
 *   - cacheKey (string, optional): Specific cache key to clear. If not provided, clears all.
 * 
 * Returns:
 *   - success (boolean): Whether operation was successful
 *   - message (string): Human-readable status message
 *   - stats (object, optional): Cache statistics after clearing
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RpcMethodHandlerFcliClearCache implements IRpcMethodHandler {
    private final ObjectMapper objectMapper;
    private final FcliRecordsCache cache;
    
    @Override
    public JsonNode execute(JsonNode params) throws RpcMethodException {
        var cacheKey = params != null && params.has("cacheKey") 
            ? params.get("cacheKey").asText() 
            : null;
        
        ObjectNode result = objectMapper.createObjectNode();
        
        if (cacheKey != null && !cacheKey.isBlank()) {
            log.debug("Clearing cache entry: cacheKey={}", cacheKey);
            var cleared = cache.clear(cacheKey);
            result.put("success", cleared);
            result.put("cacheKey", cacheKey);
            result.put("message", cleared 
                ? "Cache entry cleared successfully" 
                : "No cache entry found for this cacheKey");
        } else {
            log.debug("Clearing all cache entries");
            cache.clearAll();
            result.put("success", true);
            result.put("message", "All cache entries cleared");
        }
        
        // Add current stats
        var stats = cache.getStats();
        ObjectNode statsNode = result.putObject("stats");
        statsNode.put("cachedEntries", stats.getCachedEntries());
        statsNode.put("inProgressEntries", stats.getInProgressEntries());
        
        return result;
    }
}
