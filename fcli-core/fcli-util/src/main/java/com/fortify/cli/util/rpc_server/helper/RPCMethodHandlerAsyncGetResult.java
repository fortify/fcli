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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util._common.helper.AsyncJobManager;
import com.fortify.cli.util._common.helper.FcliExecutionResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for atomically retrieving the complete result of an async job.
 * Suitable for non-streaming commands or functions where the entire result is wanted
 * in one response. For large streaming results, prefer {@code async.getPage}.
 *
 * Method: async.getResult
 * Params:
 *   - jobId (string, required): Job ID from an {@code fcli.execute} or {@code fn.*} async call
 *   - wait (boolean, optional): If true (default), wait for completion before returning
 *   - waitTimeoutMs (integer, optional): Max wait duration in ms (default: 30000)
 *
 * Returns:
 *   - status (string): "complete", "error", "loading", or "not_found"
 *   - jobId (string): The job ID provided
 *   - exitCode (integer, optional): Exit code when complete
 *   - stderr (string, optional): Error output if any
 *   - stdout (string, optional): Stdout for non-record-producing jobs
 *   - records (array, optional): All records for record-producing jobs
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerAsyncGetResult implements IRPCMethodHandler {
    private final AsyncJobManager asyncJobManager;

    @Override
    public String description() {
        return "Atomically retrieve the complete result of an async job by jobId; use async.getPage for large streaming results";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("jobId")) {
            throw RPCMethodException.invalidParams("'jobId' parameter is required");
        }

        var jobId = params.get("jobId").asText();
        var wait = !params.has("wait") || params.get("wait").asBoolean(true);
        var waitTimeoutMs = params.has("waitTimeoutMs") ? params.get("waitTimeoutMs").asInt(30000) : 30000;

        if (jobId == null || jobId.isBlank()) {
            throw RPCMethodException.invalidParams("'jobId' cannot be empty");
        }

        log.debug("Getting result: jobId={} wait={}", jobId, wait);

        try {
            FcliExecutionResult result = null;
            if (wait) {
                result = asyncJobManager.waitForCompletion(jobId, waitTimeoutMs);
            }
            if (result == null) {
                result = asyncJobManager.getCompleted(jobId);
            }
            if (result != null) {
                return buildCompletedResponse(result, jobId);
            }

            var inProgress = asyncJobManager.getInProgress(jobId);
            if (inProgress != null) {
                return buildLoadingResponse(jobId);
            }

            return buildNotFoundResponse(jobId);
        } catch (Exception e) {
            log.error("Error getting result: jobId={}", jobId, e);
            throw RPCMethodException.internalError("Failed to get result: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildCompletedResponse(FcliExecutionResult result, String jobId) {
        var response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("status", result.getExitCode() == 0 ? "complete" : "error");
        response.put("jobId", jobId);
        response.put("exitCode", result.getExitCode());

        if (result.getStderr() != null && !result.getStderr().isBlank()) {
            response.put("stderr", result.getStderr());
        }
        if (result.getStdout() != null && !result.getStdout().isBlank()) {
            response.put("stdout", result.getStdout());
        }
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            ArrayNode recordsArray = response.putArray("records");
            result.getRecords().forEach(recordsArray::add);
        }

        return response;
    }

    private ObjectNode buildLoadingResponse(String jobId) {
        var response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("status", "loading");
        response.put("jobId", jobId);
        response.put("message", "Async job still in progress. Call again with wait=true or poll async.getResult.");
        return response;
    }

    private ObjectNode buildNotFoundResponse(String jobId) {
        var response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("status", "not_found");
        response.put("jobId", jobId);
        response.put("message", "No async job found for this jobId.");
        return response;
    }
}
