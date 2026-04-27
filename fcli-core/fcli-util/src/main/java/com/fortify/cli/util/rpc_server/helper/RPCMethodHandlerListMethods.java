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
import com.fortify.cli.common.json.JsonHelper;

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
 *     - description (string): Method description (from {@link IRPCMethodHandler#description()})
 *   - count (integer): Total number of registered methods
 *
 * @author Ruud Senden
 */
@RequiredArgsConstructor
public final class RPCMethodHandlerListMethods implements IRPCMethodHandler {
    private final Map<String, IRPCMethodHandler> methodHandlers;

    @Override
    public String description() {
        return "List available RPC methods";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        ArrayNode methods = JsonHelper.getObjectMapper().createArrayNode();

        for (var entry : methodHandlers.entrySet()) {
            ObjectNode method = JsonHelper.getObjectMapper().createObjectNode();
            method.put("name", entry.getKey());
            method.put("description", entry.getValue().description());
            methods.add(method);
        }

        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.set("methods", methods);
        result.put("count", methods.size());

        return result;
    }
}
