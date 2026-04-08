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
import com.fortify.cli.util._common.helper.RecordProducerFcliCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for starting async fcli command execution with caching.
 * 
 * Method: fcli.executeAsync
 * Params:
 *   - command (string, required): The fcli command to execute (e.g., "ssc issue list")
 * 
 * Returns:
 *   - cacheKey (string): Key to retrieve results via rpc.getPage
 *   - status (string): "started" or "cached"
 *   - message (string): Human-readable status message
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerFcliExecuteAsync implements IRPCMethodHandler {
    private final ObjectMapper objectMapper;
    private final FcliRecordsCache cache;
    
    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("command")) {
            throw RPCMethodException.invalidParams("'command' parameter is required");
        }
        
        var command = params.get("command").asText();
        if (command == null || command.isBlank()) {
            throw RPCMethodException.invalidParams("'command' cannot be empty");
        }
        
        log.debug("Starting async execution: command={}", command);
        
        try {
            var cacheKey = cache.startBackgroundCollection(new RecordProducerFcliCommand(command));

            ObjectNode result = objectMapper.createObjectNode();
            result.put("cacheKey", cacheKey);
            result.put("status", "started");
            result.put("message", "Background collection started. Use rpc.getPage with this cacheKey to retrieve results.");
            
            return result;
        } catch (Exception e) {
            log.error("Error starting async execution: {}", command, e);
            throw RPCMethodException.internalError("Failed to start async execution: " + e.getMessage(), e);
        }
    }
}
