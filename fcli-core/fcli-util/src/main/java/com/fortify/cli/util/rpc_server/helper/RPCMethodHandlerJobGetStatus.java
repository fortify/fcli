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
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.concurrent.job.AsyncJobManager;
import com.fortify.cli.common.concurrent.job.CachingJobEventListener;
import com.fortify.cli.common.json.JsonHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for querying the status of a single job.
 *
 * Method: job.getStatus
 * Params:
 *   - jobId (string, required): Job ID to query
 *
 * Returns:
 *   - jobId (string): The job ID
 *   - status (string): "running", "complete", "error", or "not_found"
 *   - description (string): Job description
 *   - completed (boolean): Whether the job has finished
 *   - exitCode (int): Exit code (0 if still running)
 *   - cached (boolean): Whether a record cache exists for this job
 *   - createdMillis (long): Job creation timestamp
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerJobGetStatus implements IRPCMethodHandler {
    private final AsyncJobManager asyncJobManager;

    @Override
    public String description() {
        return "Get the status of a single job by jobId";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("jobId")) {
            throw RPCMethodException.invalidParams("'jobId' parameter is required");
        }
        var jobId = params.get("jobId").asText();
        if (jobId == null || jobId.isBlank()) {
            throw RPCMethodException.invalidParams("'jobId' cannot be empty");
        }

        var info = asyncJobManager.getJobInfo(jobId);
        var response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("jobId", jobId);

        if (info == null) {
            response.put("status", "not_found");
        } else {
            response.put("status", !info.isCompleted() ? "running"
                    : info.getExitCode() == 0 ? "complete" : "error");
            response.put("description", info.getDescription());
            response.put("completed", info.isCompleted());
            response.put("exitCode", info.getExitCode());
            response.put("cached", getCachingListener().hasCache(jobId));
            response.put("createdMillis", info.getCreatedMillis());
        }
        return response;
    }

    private CachingJobEventListener getCachingListener() {
        return FcliExecutionContextHolder.current().getIsolationScope()
                .getOrCreateScopedState(CachingJobEventListener.class, CachingJobEventListener::new);
    }
}
