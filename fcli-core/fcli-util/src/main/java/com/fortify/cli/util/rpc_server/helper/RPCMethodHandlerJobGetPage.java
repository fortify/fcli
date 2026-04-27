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
import com.fortify.cli.util._common.helper.CachingJobEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for retrieving a page of cached records from a running or
 * completed job.
 *
 * Method: job.getPage
 * Params:
 *   - jobId (string, required): Job ID from an {@code fcli.execute} or {@code fn.call} response
 *   - offset (integer, optional): Start offset (default: 0)
 *   - limit (integer, optional): Maximum records to return (default: 100)
 *
 * Returns:
 *   - status (string): "complete", "loading", "error", or "not_found"
 *   - jobId (string): The job ID
 *   - records (array): Records for this page
 *   - pagination (object): offset, limit, loadedCount, hasMore, complete
 *   - exitCode (integer, optional): Exit code if complete
 *   - stderr (string, optional): Error output if any
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerJobGetPage implements IRPCMethodHandler {
    private final CachingJobEventListener cachingListener;

    @Override
    public String description() {
        return "Retrieve a page of records by jobId from the cache; works for all async jobs";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("jobId")) {
            throw RPCMethodException.invalidParams("'jobId' parameter is required");
        }

        var jobId = params.get("jobId").asText();
        var offset = params.has("offset") ? params.get("offset").asInt(0) : 0;
        var limit = params.has("limit") ? params.get("limit").asInt(100) : 100;

        if (jobId == null || jobId.isBlank()) {
            throw RPCMethodException.invalidParams("'jobId' cannot be empty");
        }
        if (offset < 0) {
            throw RPCMethodException.invalidParams("'offset' must be non-negative");
        }
        if (limit <= 0) {
            throw RPCMethodException.invalidParams("'limit' must be greater than 0");
        }

        log.debug("Getting page: jobId={} offset={} limit={}", jobId, offset, limit);

        var pageResult = cachingListener.getPage(jobId, offset, limit);
        return toResponse(pageResult);
    }

    private ObjectNode toResponse(CachingJobEventListener.PageResult page) {
        var response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("status", page.getStatus());
        response.put("jobId", page.getJobId());

        ArrayNode recordsArray = response.putArray("records");
        page.getRecords().forEach(recordsArray::add);

        ObjectNode pagination = response.putObject("pagination");
        pagination.put("offset", page.getOffset());
        pagination.put("limit", page.getLimit());
        pagination.put("loadedCount", page.getLoadedCount());
        pagination.put("hasMore", page.isHasMore());
        pagination.put("complete", page.isComplete());

        if (page.isComplete()) {
            response.put("exitCode", page.getExitCode());
            if (page.getStderr() != null && !page.getStderr().isBlank()) {
                response.put("stderr", page.getStderr());
            }
            if (page.getStdout() != null && !page.getStdout().isBlank()) {
                response.put("stdout", page.getStdout());
            }
        }

        return response;
    }
}
