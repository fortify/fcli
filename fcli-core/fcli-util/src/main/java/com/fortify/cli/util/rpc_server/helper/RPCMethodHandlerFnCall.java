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
import com.fortify.cli.util._common.helper.CollectingJobEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for {@code fn.call}. By default runs asynchronously: returns
 * a {@code jobId} immediately, and pushes records/completion as JSON-RPC
 * notifications.
 *
 * <p>Supports optional {@code cache}, {@code push}, and {@code wait} parameters to control
 * record caching, push notification behavior, and synchronous wait mode.</p>
 *
 * <p>When {@code wait} is specified, the handler blocks until the job completes (or the
 * timeout elapses) and returns all results inline. If the timeout elapses before
 * completion, the normal async response ({@code jobId}/{@code status: "started"}) is
 * returned instead, so the caller can fall back to polling.</p>
 *
 * Method: fn.call
 * Params:
 *   - name (string, required): Name of the exported function to call (see fn.list)
 *   - args (object, optional): Function arguments as key/value pairs
 *   - cache (object, optional): Enable record caching; use {ttl: "5m"} to specify TTL
 *   - push (boolean, optional): Enable push notifications (default: true)
 *   - wait (boolean|object, optional): Wait for completion; use true for indefinite, {timeout: "30s"} for bounded wait
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerFnCall implements IRPCMethodHandler {
    private final Map<String, ActionFunctionExecutor> functions;
    private final AsyncJobManager asyncJobManager;
    private final RPCJobEventListenerFactory listenerFactory;

    @Override
    public String description() {
        return "Call an imported function by name; by default async, or use wait param to block for results";
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
            var cacheConfig = RPCJobEventListenerFactory.parseCacheParam(params);
            var push = RPCJobEventListenerFactory.parsePushParam(params);
            var waitConfig = RPCJobEventListenerFactory.parseWaitParam(params);
            var listener = listenerFactory.createListener(cacheConfig, push);

            // When waiting, wrap the listener to collect records and signal completion
            var collector = waitConfig != null ? new CollectingJobEventListener(listener) : null;
            var effectiveListener = collector != null ? collector : listener;

            var argsNode = buildArgsNode(params);
            var task = new AsyncTaskActionFunction(executor, argsNode);
            var description = "fn:" + name;
            var jobId = asyncJobManager.startBackground(task, effectiveListener, description);

            if (collector != null) {
                return RPCWaitHelper.awaitOrFallback(collector, waitConfig, jobId, "records", cacheConfig, true);
            }
            return RPCWaitHelper.buildAsyncResponse(jobId, "records", cacheConfig);
        } catch (RPCMethodException e) {
            throw e;
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
