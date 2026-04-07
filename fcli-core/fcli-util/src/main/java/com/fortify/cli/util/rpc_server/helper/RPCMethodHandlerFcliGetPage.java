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

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.util._common.helper.FcliRecordsCache;
import com.fortify.cli.util._common.helper.FcliToolResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for retrieving a page of results from cache.
 * 
 * Method: fcli.getPage
 * Params:
 *   - cacheKey (string, required): Cache key from fcli.executeAsync
 *   - offset (integer, optional): Start offset (default: 0)
 *   - limit (integer, optional): Maximum records to return (default: 100)
 *   - wait (boolean, optional): If true, wait for completion if still loading (default: false)
 *   - waitTimeoutMs (integer, optional): Max time to wait in ms (default: 30000)
 * 
 * Returns:
 *   - status (string): "complete", "partial", "loading", "not_found", or "error"
 *   - records (array): Array of record objects for this page
 *   - pagination (object): Pagination metadata
 *   - loadedCount (integer): Number of records loaded so far
 *   - exitCode (integer, optional): Command exit code if complete
 *   - stderr (string, optional): Error output if any
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerFcliGetPage implements IRPCMethodHandler {
    private final ObjectMapper objectMapper;
    private final FcliRecordsCache cache;
    
    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("cacheKey")) {
            throw RPCMethodException.invalidParams("'cacheKey' parameter is required");
        }
        
        var cacheKey = params.get("cacheKey").asText();
        var offset = params.has("offset") ? params.get("offset").asInt(0) : 0;
        var limit = params.has("limit") ? params.get("limit").asInt(100) : 100;
        var wait = params.has("wait") && params.get("wait").asBoolean(false);
        var waitTimeoutMs = params.has("waitTimeoutMs") ? params.get("waitTimeoutMs").asInt(30000) : 30000;
        
        if (cacheKey == null || cacheKey.isBlank()) {
            throw RPCMethodException.invalidParams("'cacheKey' cannot be empty");
        }
        
        if (offset < 0) {
            throw RPCMethodException.invalidParams("'offset' must be non-negative");
        }
        
        if (limit <= 0) {
            throw RPCMethodException.invalidParams("'limit' must be greater than 0");
        }
        
        log.debug("Getting page: cacheKey={} offset={} limit={} wait={}", cacheKey, offset, limit, wait);
        
        try {
            // If wait requested, wait for completion first
            if (wait) {
                var waitResult = cache.waitForCompletion(cacheKey, waitTimeoutMs);
                if (waitResult != null) {
                    return buildCompletedResponse(waitResult, offset, limit, cacheKey);
                }
            }
            
            // Check if we have a cached complete result
            var cached = cache.getCached(cacheKey);
            if (cached != null) {
                return buildCompletedResponse(cached, offset, limit, cacheKey);
            }
            
            // Check if loading is in progress
            var inProgress = cache.getInProgress(cacheKey);
            if (inProgress != null) {
                return buildInProgressResponse(inProgress, offset, limit);
            }
            
            // Not found
            return buildNotFoundResponse(cacheKey);
            
        } catch (Exception e) {
            log.error("Error getting page: cacheKey={}", cacheKey, e);
            throw RPCMethodException.internalError("Failed to get page: " + e.getMessage(), e);
        }
    }
    
    private ObjectNode buildCompletedResponse(FcliToolResult result, int offset, int limit, String cacheKey) {
        var allRecords = result.getRecords();
        var totalRecords = allRecords != null ? allRecords.size() : 0;
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", result.getExitCode() == 0 ? "complete" : "error");
        response.put("cacheKey", cacheKey);
        response.put("exitCode", result.getExitCode());
        
        if (result.getStderr() != null && !result.getStderr().isBlank()) {
            response.put("stderr", result.getStderr());
        }
        
        // Get the requested page
        var endIndex = Math.min(offset + limit, totalRecords);
        List<JsonNode> pageRecords = offset >= totalRecords 
            ? List.of() 
            : allRecords.subList(offset, endIndex);
        
        ArrayNode recordsArray = response.putArray("records");
        pageRecords.forEach(recordsArray::add);
        
        // Pagination metadata
        ObjectNode pagination = response.putObject("pagination");
        pagination.put("offset", offset);
        pagination.put("limit", limit);
        pagination.put("totalRecords", totalRecords);
        pagination.put("totalPages", (int) Math.ceil((double) totalRecords / limit));
        pagination.put("hasMore", offset + limit < totalRecords);
        pagination.put("complete", true);
        if (offset + limit < totalRecords) {
            pagination.put("nextOffset", offset + limit);
        }
        
        response.put("loadedCount", totalRecords);
        
        return response;
    }
    
    private ObjectNode buildInProgressResponse(FcliRecordsCache.InProgressEntry inProgress, int offset, int limit) {
        var loadedRecords = inProgress.getRecordsSnapshot();
        var loadedCount = loadedRecords.size();
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", inProgress.isCompleted() ? "complete" : "loading");
        response.put("cacheKey", inProgress.getCacheKey());
        response.put("loadedCount", loadedCount);
        
        if (inProgress.isCompleted()) {
            response.put("exitCode", inProgress.getExitCode());
            if (inProgress.getStderr() != null && !inProgress.getStderr().isBlank()) {
                response.put("stderr", inProgress.getStderr());
            }
        }
        
        // Return available records within requested range
        var endIndex = Math.min(offset + limit, loadedCount);
        List<JsonNode> pageRecords = offset >= loadedCount 
            ? List.of() 
            : loadedRecords.subList(offset, endIndex);
        
        ArrayNode recordsArray = response.putArray("records");
        pageRecords.forEach(recordsArray::add);
        
        // Pagination metadata (partial)
        ObjectNode pagination = response.putObject("pagination");
        pagination.put("offset", offset);
        pagination.put("limit", limit);
        pagination.put("hasMore", loadedCount > offset + limit || !inProgress.isCompleted());
        pagination.put("complete", inProgress.isCompleted());
        if (loadedCount > offset + limit) {
            pagination.put("nextOffset", offset + limit);
        }
        pagination.put("guidance", "Collection in progress. Call again with wait=true to wait for completion, or poll periodically.");
        
        return response;
    }
    
    private ObjectNode buildNotFoundResponse(String cacheKey) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "not_found");
        response.put("cacheKey", cacheKey);
        response.put("message", "No cached result or in-progress collection found for this cacheKey. Use fcli.executeAsync to start a new collection.");
        response.putArray("records");
        return response;
    }
}
