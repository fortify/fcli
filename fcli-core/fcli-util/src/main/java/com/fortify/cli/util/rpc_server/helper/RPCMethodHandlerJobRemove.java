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
 * RPC method handler for removing a completed job from tracking and clearing
 * its record cache if present.
 *
 * Method: job.remove
 * Params:
 *   - jobId (string, required): Job ID to remove
 *
 * Returns:
 *   - success (boolean): Whether removal was successful
 *   - jobId (string): The job ID provided
 *   - message (string): Human-readable status message
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerJobRemove implements IRPCMethodHandler {
    private final AsyncJobManager asyncJobManager;

    @Override
    public String description() {
        return "Remove a completed job from tracking and clear its record cache";
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

        log.debug("Removing job: jobId={}", jobId);

        var info = asyncJobManager.getJobInfo(jobId);
        if (info == null) {
            return result(false, jobId, "No job found for this jobId");
        }
        if (!info.isCompleted()) {
            return result(false, jobId, "Cannot remove a running job; cancel it first");
        }

        asyncJobManager.removeJob(jobId);
        getCachingListener().remove(jobId);
        return result(true, jobId, "Job removed successfully");
    }

    private CachingJobEventListener getCachingListener() {
        return FcliExecutionContextHolder.current().getIsolationScope()
                .getOrCreateScopedState(CachingJobEventListener.class, CachingJobEventListener::new);
    }

    private static JsonNode result(boolean success, String jobId, String message) {
        var response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("success", success);
        response.put("jobId", jobId);
        response.put("message", message);
        return response;
    }
}
