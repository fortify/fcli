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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util._common.helper.AsyncJobManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for clearing cache entries.
 *
 * Method: async.clear
 * Params:
 *   - jobId (string, optional): Specific async job ID to clear. If omitted, clears all.
 *
 * Returns:
 *   - success (boolean): Whether the operation was successful
 *   - message (string): Human-readable status message
 *   - stats (object): Job manager statistics after clearing
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerAsyncClear implements IRPCMethodHandler {
    private static final ObjectMapper OM = JsonHelper.getObjectMapper();
    private final AsyncJobManager asyncJobManager;

    @Override
    public String description() {
        return "Clear async job entries (specific jobId, or all if omitted)";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        var jobId = params != null && params.has("jobId")
            ? params.get("jobId").asText()
            : null;

        ObjectNode result = OM.createObjectNode();

        if (jobId != null && !jobId.isBlank()) {
            log.debug("Clearing async job entry: jobId={}", jobId);
            var cleared = asyncJobManager.clear(jobId);
            result.put("success", cleared);
            result.put("jobId", jobId);
            result.put("message", cleared
                ? "Async job entry cleared successfully"
                : "No async job entry found for this jobId");
        } else {
            log.debug("Clearing all async job entries");
            asyncJobManager.clearAll();
            result.put("success", true);
            result.put("message", "All async job entries cleared");
        }

        var stats = asyncJobManager.getStats();
        ObjectNode statsNode = result.putObject("stats");
        statsNode.put("completedEntries", stats.getCompletedEntries());
        statsNode.put("runningEntries", stats.getRunningEntries());

        return result;
    }
}
