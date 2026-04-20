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
import com.fortify.cli.util._common.helper.AsyncJobManager;
import com.fortify.cli.util._common.helper.AsyncTaskFcliCommand;
import com.fortify.cli.util._common.helper.CollectingJobEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for executing fcli commands. By default runs asynchronously:
 * returns a {@code jobId} immediately, and pushes records/progress/completion
 * as JSON-RPC notifications.
 *
 * <p>Supports optional {@code cache}, {@code push}, and {@code wait} parameters to control
 * record caching, push notification behavior, and synchronous wait mode.</p>
 *
 * <p>When {@code wait} is specified, the handler blocks until the job completes (or the
 * timeout elapses) and returns all results inline. If the timeout elapses before
 * completion, the normal async response ({@code jobId}/{@code status: "started"}) is
 * returned instead, so the caller can fall back to polling.</p>
 *
 * Method: fcli.execute
 * Params:
 *   - command (string, required): The fcli command to execute (e.g., "ssc appversion list")
 *   - collectRecords (boolean, optional): If true, collect structured records; if false, collect stdout (default: true)
 *   - cache (object, optional): Enable record caching; use {ttl: "5m"} to specify TTL
 *   - push (boolean, optional): Enable push notifications (default: true)
 *   - wait (boolean|object, optional): Wait for completion; use true for indefinite, {timeout: "30s"} for bounded wait
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerFcliExecute implements IRPCMethodHandler {
    private final AsyncJobManager asyncJobManager;
    private final RPCJobEventListenerFactory listenerFactory;

    @Override
    public String description() {
        return "Execute an fcli command; by default async with push notifications, or use wait param to block for results";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("command")) {
            throw RPCMethodException.invalidParams("'command' parameter is required");
        }

        var command = params.get("command").asText();
        var collectRecords = !params.has("collectRecords") || params.get("collectRecords").asBoolean(true);

        if (command == null || command.isBlank()) {
            throw RPCMethodException.invalidParams("'command' cannot be empty");
        }

        var cacheConfig = RPCJobEventListenerFactory.parseCacheParam(params);
        var push = RPCJobEventListenerFactory.parsePushParam(params);
        var waitConfig = RPCJobEventListenerFactory.parseWaitParam(params);
        var listener = listenerFactory.createListener(cacheConfig, push);

        // When waiting, wrap the listener to collect records and signal completion
        var collector = waitConfig != null ? new CollectingJobEventListener(listener) : null;
        var effectiveListener = collector != null ? collector : listener;
        var jobType = collectRecords ? "records" : "stdout";

        log.debug("Executing fcli command: {} (collectRecords={}, cached={}, push={}, wait={})",
                command, collectRecords, cacheConfig != null, push, waitConfig != null);

        var task = new AsyncTaskFcliCommand(command, collectRecords);
        var description = "fcli " + command;
        var jobId = asyncJobManager.startBackground(task, effectiveListener, description);

        if (collector != null) {
            return RPCWaitHelper.awaitOrFallback(collector, waitConfig, jobId, jobType, cacheConfig, collectRecords);
        }
        return RPCWaitHelper.buildAsyncResponse(jobId, jobType, cacheConfig);
    }
}
