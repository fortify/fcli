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

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util._common.helper.AsyncJobManager;
import com.fortify.cli.util._common.helper.FcliExecutionResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for retrieving a page of results from cache.
 *
 * Method: async.getPage
 * Params:
 *   - jobId (string, required): Job ID from an {@code fcli.execute} or {@code fn.*} async call
 *   - offset (integer, optional): Start offset (default: 0)
 *   - limit (integer, optional): Maximum records to return (default: 100)
 *   - wait (boolean, optional): If true, wait for completion if still loading (default: false)
 *   - waitTimeoutMs (integer, optional): Max time to wait in ms (default: 30000)
 *
 * Returns:
 *   - status (string): "complete", "loading", "not_found", or "error"
 *   - records (array): Array of record objects for this page
 *   - pagination (object): Pagination metadata
 *   - loadedCount (integer): Number of records loaded so far
 *   - exitCode (integer, optional): Exit code if complete
 *   - stderr (string, optional): Error output if any
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerAsyncGetPage implements IRPCMethodHandler {
    private final AsyncJobManager asyncJobManager;

    @Override
    public String description() {
        return "Retrieve a page of results by jobId; works for all async jobs (commands and functions)";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("jobId")) {
            throw RPCMethodException.invalidParams("'jobId' parameter is required");
        }

        var jobId = params.get("jobId").asText();
        var offset = params.has("offset") ? params.get("offset").asInt(0) : 0;
        var limit = params.has("limit") ? params.get("limit").asInt(100) : 100;
        var wait = params.has("wait") && params.get("wait").asBoolean(false);
        var waitTimeoutMs = params.has("waitTimeoutMs") ? params.get("waitTimeoutMs").asInt(30000) : 30000;

        if (jobId == null || jobId.isBlank()) {
            throw RPCMethodException.invalidParams("'jobId' cannot be empty");
        }
        if (offset < 0) {
            throw RPCMethodException.invalidParams("'offset' must be non-negative");
        }
        if (limit <= 0) {
            throw RPCMethodException.invalidParams("'limit' must be greater than 0");
        }

        log.debug("Getting page: jobId={} offset={} limit={} wait={}", jobId, offset, limit, wait);

        try {
            if (wait) {
                var waitResult = asyncJobManager.waitForCompletion(jobId, waitTimeoutMs);
                if (waitResult != null) {
                    return buildCompletedResponse(waitResult, offset, limit, jobId);
                }
            }

            var completed = asyncJobManager.getCompleted(jobId);
            if (completed != null) {
                return buildCompletedResponse(completed, offset, limit, jobId);
            }

            var inProgress = asyncJobManager.getInProgress(jobId);
            if (inProgress != null) {
                return buildInProgressResponse(inProgress, offset, limit);
            }

            return buildNotFoundResponse(jobId);
        } catch (Exception e) {
            log.error("Error getting page: jobId={}", jobId, e);
            throw RPCMethodException.internalError("Failed to get page: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildCompletedResponse(FcliExecutionResult result, int offset, int limit, String jobId) {
        var allRecords = result.getRecords() != null ? result.getRecords() : List.<JsonNode>of();
        var totalRecords = allRecords.size();

        ObjectNode response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("status", result.getExitCode() == 0 ? "complete" : "error");
        response.put("jobId", jobId);
        response.put("exitCode", result.getExitCode());

        if (result.getStderr() != null && !result.getStderr().isBlank()) {
            response.put("stderr", result.getStderr());
        }
        if (result.getStdout() != null && !result.getStdout().isBlank()) {
            response.put("stdout", result.getStdout());
        }

        var endIndex = Math.min(offset + limit, totalRecords);
        List<JsonNode> pageRecords = offset >= totalRecords
            ? List.of()
            : allRecords.subList(offset, endIndex);

        ArrayNode recordsArray = response.putArray("records");
        pageRecords.forEach(recordsArray::add);

        ObjectNode pagination = response.putObject("pagination");
        pagination.put("offset", offset);
        pagination.put("limit", limit);
        pagination.put("totalRecords", totalRecords);
        pagination.put("totalPages", (int) Math.ceil((double) totalRecords / limit));
        pagination.put("hasMore", offset + limit < totalRecords);
        pagination.put("complete", true);
        if (offset + limit < totalRecords) {
            pagination.put("nextOffset", offset + limit);
        }

        response.put("loadedCount", totalRecords);

        return response;
    }

    private ObjectNode buildInProgressResponse(AsyncJobManager.InProgressEntry inProgress, int offset, int limit) {
        var loadedRecords = inProgress.getRecordsSnapshot();
        var loadedCount = loadedRecords.size();

        ObjectNode response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("status", !inProgress.isCompleted() ? "loading" : inProgress.getExitCode() == 0 ? "complete" : "error");
        response.put("jobId", inProgress.getJobId());
        response.put("loadedCount", loadedCount);

        if (inProgress.isCompleted()) {
            response.put("exitCode", inProgress.getExitCode());
            if (inProgress.getStderr() != null && !inProgress.getStderr().isBlank()) {
                response.put("stderr", inProgress.getStderr());
            }
        }

        var endIndex = Math.min(offset + limit, loadedCount);
        List<JsonNode> pageRecords = offset >= loadedCount
            ? List.of()
            : loadedRecords.subList(offset, endIndex);

        ArrayNode recordsArray = response.putArray("records");
        pageRecords.forEach(recordsArray::add);

        ObjectNode pagination = response.putObject("pagination");
        pagination.put("offset", offset);
        pagination.put("limit", limit);
        pagination.put("hasMore", loadedCount > offset + limit || !inProgress.isCompleted());
        pagination.put("complete", inProgress.isCompleted());
        if (loadedCount > offset + limit) {
            pagination.put("nextOffset", offset + limit);
        }
        pagination.put("guidance", "Collection in progress. Call again with wait=true to wait for completion, or poll periodically.");

        return response;
    }

    private ObjectNode buildNotFoundResponse(String jobId) {
        ObjectNode response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("status", "not_found");
        response.put("jobId", jobId);
        response.put("message", "No async job found for this jobId.");
        response.putArray("records");
        return response;
    }
}
