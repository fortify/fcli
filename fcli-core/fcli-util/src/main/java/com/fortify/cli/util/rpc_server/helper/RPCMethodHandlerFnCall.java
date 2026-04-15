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

import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.ActionStepRecordsForEach.IActionStepForEachProcessor;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util._common.helper.AsyncJobManager;
import com.fortify.cli.util._common.helper.AsyncTaskActionFunction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for {@code fn.call}.
 * <p>
 * Dispatches to an imported action function by name, keeping RPC-level parameters
 * ({@code name}, {@code async}) cleanly separate from function arguments ({@code args}).
 *
 * Method: fn.call
 * Params:
 *   - name (string, required): Name of the exported function to call (see fn.list)
 *   - args (object, optional): Function arguments as key/value pairs
 *   - async (boolean, optional): If true, run in background and return a jobId (default: false)
 *
 * Synchronous response (async=false):
 *   - For non-streaming functions: the function's return value directly
 *   - For streaming functions: {@code {"records": [...]}} with all collected records
 *
 * Async response (async=true):
 *   - jobId (string): Key for use with async.getPage or async.getResult
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

    @Override
    public String description() {
        return "Call an imported function by name; use fn.list to see available functions";
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
        log.debug("Executing action function: {}", name);
        try {
            var argsNode = buildArgsNode(params);
            var async = params.has("async") && params.get("async").asBoolean(false);
            if (async) {
                return executeAsync(executor, argsNode);
            } else if (executor.getFunction().isStreaming()) {
                return executeStreamingSync(executor, argsNode);
            } else {
                return executeNonStreamingSync(executor, argsNode);
            }
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

    private JsonNode executeNonStreamingSync(ActionFunctionExecutor executor, ObjectNode argsNode) {
        var result = executor.execute(argsNode);
        if (result instanceof JsonNode jn) {
            return jn;
        } else if (result != null) {
            return JsonHelper.getObjectMapper().valueToTree(result);
        }
        return JsonHelper.getObjectMapper().createObjectNode();
    }

    private JsonNode executeStreamingSync(ActionFunctionExecutor executor, ObjectNode argsNode) {
        var records = new ArrayList<JsonNode>();
        var result = executor.execute(argsNode);
        if (result instanceof IActionStepForEachProcessor p) {
            p.process(node -> { records.add(node); return true; });
        }
        var response = JsonHelper.getObjectMapper().createObjectNode();
        ArrayNode recordsArray = response.putArray("records");
        records.forEach(recordsArray::add);
        return response;
    }

    private JsonNode executeAsync(ActionFunctionExecutor executor, ObjectNode argsNode) {
        var task = new AsyncTaskActionFunction(executor, argsNode);
        var jobId = asyncJobManager.startBackground(task);
        var response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("jobId", jobId);
        response.put("status", "started");
        response.put("jobType", "records");
        log.debug("Started async function: fn={} jobId={}", executor.getFunction().getKey(), jobId);
        return response;
    }
}
