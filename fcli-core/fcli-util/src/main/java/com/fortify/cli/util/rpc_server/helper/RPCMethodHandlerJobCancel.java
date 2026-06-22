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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.concurrent.job.AsyncJobManager;
import com.fortify.cli.common.json.JsonHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for cancelling a running job.
 *
 * Method: job.cancel
 * Params:
 *   - jobId (string, required): Job ID of the async job to cancel
 *
 * Returns:
 *   - success (boolean): Whether cancellation was successful
 *   - jobId (string): The job ID provided
 *   - message (string): Human-readable status message
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerJobCancel implements IRPCMethodHandler {
    private final AsyncJobManager asyncJobManager;

    @Override
    public String description() {
        return "Cancel a running async job by jobId";
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

        log.debug("Cancelling job: jobId={}", jobId);

        var cancelled = asyncJobManager.cancel(jobId);

        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("success", cancelled);
        result.put("jobId", jobId);
        result.put("message", cancelled
            ? "Job cancelled successfully"
            : "No running job found for this jobId");

        return result;
    }
}
