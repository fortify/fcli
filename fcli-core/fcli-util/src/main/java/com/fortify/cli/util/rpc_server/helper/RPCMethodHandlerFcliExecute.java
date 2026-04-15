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
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util._common.helper.AsyncJobManager;
import com.fortify.cli.util._common.helper.AsyncTaskFcliCommand;
import com.fortify.cli.util._common.helper.IJobEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for executing fcli commands. Always runs asynchronously:
 * returns a {@code jobId} immediately, and pushes records/progress/completion
 * as JSON-RPC notifications via the configured {@link IJobEventListener}.
 *
 * Method: fcli.execute
 * Params:
 *   - command (string, required): The fcli command to execute (e.g., "ssc appversion list")
 *   - collectRecords (boolean, optional): If true, collect structured records; if false, collect stdout (default: false)
 *
 * Response:
 *   - jobId (string): Identifier for tracking via job.getPage / job.cancel / job.list
 *   - status (string): "started"
 *   - jobType (string): "records" or "stdout"
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerFcliExecute implements IRPCMethodHandler {
    private final AsyncJobManager asyncJobManager;
    private final IJobEventListener listener;

    @Override
    public String description() {
        return "Execute an fcli command asynchronously; results are pushed as job.records notifications, or use job.getPage to retrieve pages";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("command")) {
            throw RPCMethodException.invalidParams("'command' parameter is required");
        }

        var command = params.get("command").asText();
        var collectRecords = params.has("collectRecords") && params.get("collectRecords").asBoolean(false);

        if (command == null || command.isBlank()) {
            throw RPCMethodException.invalidParams("'command' cannot be empty");
        }

        log.debug("Executing fcli command (async): {} (collectRecords={})", command, collectRecords);

        var task = new AsyncTaskFcliCommand(command, collectRecords);
        var description = "fcli " + command;
        var jobId = asyncJobManager.startBackground(task, listener, description);

        var response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("jobId", jobId);
        response.put("status", "started");
        response.put("jobType", collectRecords ? "records" : "stdout");
        return response;
    }
}
