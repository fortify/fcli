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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util._common.helper.AsyncJobManager;
import com.fortify.cli.util._common.helper.AsyncTaskActionFunction;
import com.fortify.cli.util._common.helper.IJobEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for {@code fn.call}. Always runs asynchronously: returns
 * a {@code jobId} immediately, and pushes records/completion as JSON-RPC
 * notifications via the configured {@link IJobEventListener}.
 *
 * Method: fn.call
 * Params:
 *   - name (string, required): Name of the exported function to call (see fn.list)
 *   - args (object, optional): Function arguments as key/value pairs
 *
 * Response:
 *   - jobId (string): Identifier for tracking via job.getPage / job.cancel / job.list
 *   - status (string): "started"
 *   - jobType (string): "records"
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerFnCall implements IRPCMethodHandler {
    private final Map<String, ActionFunctionExecutor> functions;
    private final AsyncJobManager asyncJobManager;
    private final IJobEventListener listener;

    @Override
    public String description() {
        return "Call an imported function by name (always async); use fn.list to see available functions";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("name")) {
            throw RPCMethodException.invalidParams("'name' parameter is required");
        }
        var name = params.get("name").asText();
        var executor = functions.get(name);
        if (executor == null) {
            throw RPCMethodException.methodNotFound("Function not found: " + name);
        }
        log.debug("Executing action function (async): {}", name);
        try {
            var argsNode = buildArgsNode(params);
            var task = new AsyncTaskActionFunction(executor, argsNode);
            var description = "fn:" + name;
            var jobId = asyncJobManager.startBackground(task, listener, description);

            var response = JsonHelper.getObjectMapper().createObjectNode();
            response.put("jobId", jobId);
            response.put("status", "started");
            response.put("jobType", "records");
            return response;
        } catch (Exception e) {
            log.error("Error executing action function: {}", name, e);
            throw RPCMethodException.internalError("Function execution failed: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildArgsNode(JsonNode params) {
        var argsNode = params.get("args");
        if (argsNode instanceof ObjectNode on) {
            return on;
        }
        return JsonHelper.getObjectMapper().createObjectNode();
    }
}
