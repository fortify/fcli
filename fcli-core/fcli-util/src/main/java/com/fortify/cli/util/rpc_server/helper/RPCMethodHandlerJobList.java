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
import com.fortify.cli.common.concurrent.job.AsyncJobManager;
import com.fortify.cli.common.json.JsonHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for listing all tracked jobs.
 *
 * Method: job.list
 * Params: (none)
 *
 * Returns:
 *   - jobs (array): Array of job info objects with jobId, description, completed, exitCode, createdMillis
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerJobList implements IRPCMethodHandler {
    private final AsyncJobManager asyncJobManager;

    @Override
    public String description() {
        return "List all tracked async jobs with their current status";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        var jobInfos = asyncJobManager.listJobs();

        ObjectNode response = JsonHelper.getObjectMapper().createObjectNode();
        ArrayNode jobsArray = response.putArray("jobs");

        for (var info : jobInfos) {
            ObjectNode jobNode = JsonHelper.getObjectMapper().createObjectNode();
            jobNode.put("jobId", info.getJobId());
            jobNode.put("description", info.getDescription());
            jobNode.put("completed", info.isCompleted());
            jobNode.put("exitCode", info.getExitCode());
            jobNode.put("createdMillis", info.getCreatedMillis());
            jobsArray.add(jobNode);
        }

        response.put("totalJobs", jobInfos.size());
        return response;
    }
}
