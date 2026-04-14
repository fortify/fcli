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
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.OutputHelper.Result;
import com.fortify.cli.util._common.helper.AsyncJobManager;
import com.fortify.cli.util._common.helper.AsyncTaskFcliCommand;
import com.fortify.cli.util._common.helper.FcliRunnerHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for executing fcli commands.
 *
 * Method: fcli.execute
 * Params:
 *   - command (string, required): The fcli command to execute (e.g., "ssc appversion list")
 *   - collectRecords (boolean, optional): If true, collect structured records; if false, collect stdout (default: false)
 *   - async (boolean, optional): If true, run in background and return a jobId (default: false)
 *
 * Synchronous response (async=false):
 *   - exitCode (integer): The command exit code
 *   - records (array, optional): All records when collectRecords=true
 *   - stdout (string, optional): Standard output when collectRecords=false
 *   - stderr (string, optional): Standard error output
 *
 * Async response (async=true):
 *   - jobId (string): Key for use with async.getPage or async.getResult
 *   - status (string): "started"
 *   - jobType (string): "records" or "stdout"
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerFcliExecute implements IRPCMethodHandler {
    private final AsyncJobManager asyncJobManager;

    @Override
    public String description() {
        return "Execute an fcli command, optionally in the background (async=true); use async.getPage or async.getResult to retrieve results";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("command")) {
            throw RPCMethodException.invalidParams("'command' parameter is required");
        }

        var command = params.get("command").asText();
        var collectRecords = params.has("collectRecords") && params.get("collectRecords").asBoolean(false);
        var async = params.has("async") && params.get("async").asBoolean(false);

        if (command == null || command.isBlank()) {
            throw RPCMethodException.invalidParams("'command' cannot be empty");
        }

        log.debug("Executing fcli command: {} (collectRecords={} async={})", command, collectRecords, async);

        try {
            if (async) {
                return executeAsync(command, collectRecords);
            } else if (collectRecords) {
                return executeWithRecords(command);
            } else {
                return executeWithStdout(command);
            }
        } catch (Exception e) {
            log.error("Error executing fcli command: {}", command, e);
            throw RPCMethodException.internalError("Command execution failed: " + e.getMessage(), e);
        }
    }

    private JsonNode executeAsync(String command, boolean collectRecords) {
        var task = new AsyncTaskFcliCommand(command, collectRecords);
        var jobId = asyncJobManager.startBackground(task);

        var response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("jobId", jobId);
        response.put("status", "started");
        response.put("jobType", collectRecords ? "records" : "stdout");
        log.debug("Started async command: command={} jobId={}", command, jobId);
        return response;
    }

    private JsonNode executeWithStdout(String command) {
        var result = FcliRunnerHelper.collectStdout(command);
        return buildResponse(result, null);
    }

    private JsonNode executeWithRecords(String command) {
        var allRecords = new ArrayList<JsonNode>();
        var result = FcliRunnerHelper.collectRecords(command, allRecords::add);
        return buildResponse(result, allRecords);
    }

    private ObjectNode buildResponse(Result result, List<JsonNode> records) {
        var response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("exitCode", result.getExitCode());

        if (records != null) {
            ArrayNode recordsArray = response.putArray("records");
            records.forEach(recordsArray::add);
            response.put("totalRecords", records.size());
        } else {
            response.put("stdout", result.getOut());
        }

        if (result.getErr() != null && !result.getErr().isBlank()) {
            response.put("stderr", result.getErr());
        }

        return response;
    }
}
