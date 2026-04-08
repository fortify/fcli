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

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util._common.helper.FcliRecordsCache;
import com.fortify.cli.util._common.helper.RecordProducerActionFunction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler that wraps an {@link ActionFunctionExecutor} as a JSON-RPC method.
 * All exported functions from imported action files are registered as RPC methods.
 * <p>
 * Non-streaming functions are executed synchronously and the result is returned directly.
 * Streaming functions behave like {@code fcli.executeAsync}: a background collection is
 * started and a {@code cacheKey} is returned for use with {@code rpc.getPage}.
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerActionFunction implements IRPCMethodHandler {
    private final ActionFunctionExecutor executor;
    private final FcliRecordsCache cache;

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        log.debug("Executing action function: {}", executor.getFunction().getKey());
        try {
            var argsNode = params instanceof ObjectNode on ? on : JsonHelper.getObjectMapper().createObjectNode();
            if (executor.getFunction().isStreaming()) {
                return executeStreaming(argsNode);
            }
            return executeSync(argsNode);
        } catch (Exception e) {
            log.error("Error executing action function: {}", executor.getFunction().getKey(), e);
            throw RPCMethodException.internalError("Function execution failed: " + e.getMessage(), e);
        }
    }

    private JsonNode executeSync(ObjectNode argsNode) {
        var result = executor.execute(argsNode);
        if (result instanceof JsonNode jn) {
            return jn;
        } else if (result != null) {
            return JsonHelper.getObjectMapper().valueToTree(result);
        }
        return JsonHelper.getObjectMapper().createObjectNode();
    }

    private JsonNode executeStreaming(ObjectNode argsNode) {
        var cacheKey = UUID.randomUUID().toString();
        cache.getOrStartBackground(cacheKey, false, new RecordProducerActionFunction(executor, argsNode));
        var response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("cacheKey", cacheKey);
        response.put("status", "started");
        response.put("message", "Background collection started. Use rpc.getPage with this cacheKey to retrieve results.");
        log.debug("Started streaming function background collection: fn={} cacheKey={}",
            executor.getFunction().getKey(), cacheKey);
        return response;
    }
}
