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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.ActionStepRecordsForEach.IActionStepForEachProcessor;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.json.JsonHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler that wraps an {@link ActionFunctionExecutor} as a JSON-RPC method.
 * All exported functions from imported action files are registered as RPC methods.
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerActionFunction implements IRPCMethodHandler {
    private final ActionFunctionExecutor executor;

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        log.debug("Executing action function: {}", executor.getFunction().getKey());
        try {
            var argsNode = params instanceof ObjectNode on ? on : JsonHelper.getObjectMapper().createObjectNode();
            var result = executor.execute(argsNode);
            if (result instanceof JsonNode jn) {
                return jn;
            } else if (result instanceof IActionStepForEachProcessor processor) {
                // Collect streaming function results into an ArrayNode
                var arrayNode = JsonHelper.getObjectMapper().createArrayNode();
                processor.process(node -> { arrayNode.add(node); return true; });
                return arrayNode;
            } else if (result != null) {
                return JsonHelper.getObjectMapper().valueToTree(result);
            }
            return JsonHelper.getObjectMapper().createObjectNode();
        } catch (Exception e) {
            log.error("Error executing action function: {}", executor.getFunction().getKey(), e);
            throw RPCMethodException.internalError(
                    "Function execution failed: " + e.getMessage(), e);
        }
    }
}
