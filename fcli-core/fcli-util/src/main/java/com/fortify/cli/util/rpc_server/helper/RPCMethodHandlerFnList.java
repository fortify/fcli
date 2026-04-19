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

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.ActionFunctionArg;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.json.JsonHelper;

import lombok.RequiredArgsConstructor;

/**
 * RPC method handler for {@code fn.list}.
 * <p>
 * Lists all exported functions available for use with {@code fn.call}.
 *
 * Method: fn.list
 * Params: none
 *
 * Returns:
 *   - functions (array): Array of function descriptors with:
 *     - name (string): Function name (pass as {@code name} to fn.call)
 *     - description (string): Function description (if defined)
 *     - streaming (boolean): Whether the function streams records
 *   - count (integer): Total number of available functions
 *
 * @author Ruud Senden
 */
@RequiredArgsConstructor
public final class RPCMethodHandlerFnList implements IRPCMethodHandler {
    private final Map<String, ActionFunctionExecutor> functions;

    @Override
    public String description() {
        return "List available imported functions for use with fn.call";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        ArrayNode fns = JsonHelper.getObjectMapper().createArrayNode();
        for (var entry : functions.entrySet()) {
            var function = entry.getValue().getFunction();
            ObjectNode desc = JsonHelper.getObjectMapper().createObjectNode();
            desc.put("name", entry.getKey());
            if (function.getDescription() != null) {
                desc.put("description", function.getDescription());
            }
            desc.put("streaming", function.isStreaming());
            desc.set("args", buildArgsDescriptor(function.getArgsOrEmpty()));
            fns.add(desc);
        }
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.set("functions", fns);
        result.put("count", fns.size());
        return result;
    }

    private static ObjectNode buildArgsDescriptor(Map<String, ActionFunctionArg> args) {
        var node = JsonHelper.getObjectMapper().createObjectNode();
        for (var entry : args.entrySet()) {
            var argDef = entry.getValue();
            var argNode = JsonHelper.getObjectMapper().createObjectNode();
            argNode.put("type", argDef.getType() != null ? argDef.getType() : "string");
            argNode.put("required", Boolean.TRUE.equals(argDef.getRequired()));
            if (argDef.getDescription() != null) {
                argNode.put("description", argDef.getDescription());
            }
            node.set(entry.getKey(), argNode);
        }
        return node;
    }
}
