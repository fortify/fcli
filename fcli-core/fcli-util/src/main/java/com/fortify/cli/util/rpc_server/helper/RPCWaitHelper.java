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
import com.fortify.cli.common.concurrent.job.CollectingJobEventListener;
import com.fortify.cli.common.json.JsonHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * Shared helper for wait-mode logic in RPC method handlers. Handles awaiting
 * job completion (with optional timeout) and building the appropriate response
 * (synchronous result or async fallback).
 *
 * @author Ruud Senden
 */
@Slf4j
final class RPCWaitHelper {

    private RPCWaitHelper() {}

    /**
     * Wait for the job to complete and return results inline, or fall back to the
     * standard async response if the timeout elapses.
     */
    static JsonNode awaitOrFallback(CollectingJobEventListener collector,
                                    RPCJobEventListenerFactory.WaitConfig waitConfig,
                                    String jobId, String jobType,
                                    RPCJobEventListenerFactory.CacheConfig cacheConfig,
                                    boolean includeRecords) throws RPCMethodException {
        try {
            boolean completed;
            if (waitConfig.hasTimeout()) {
                completed = collector.await(waitConfig.timeout());
            } else {
                collector.await();
                completed = true;
            }

            if (completed) {
                return buildSyncResponse(collector, jobType, includeRecords);
            }

            log.debug("Wait timed out for job {}, falling back to async response", jobId);
            return buildAsyncResponse(jobId, jobType, cacheConfig);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw RPCMethodException.internalError("Interrupted while waiting for job completion", e);
        }
    }

    /**
     * Build the standard async response returned when no wait is requested
     * or when a wait times out.
     */
    static JsonNode buildAsyncResponse(String jobId, String jobType,
                                       RPCJobEventListenerFactory.CacheConfig cacheConfig) {
        var response = JsonHelper.getObjectMapper().createObjectNode();
        response.put("jobId", jobId);
        response.put("status", "started");
        response.put("jobType", jobType);
        response.put("cached", cacheConfig != null);
        return response;
    }

    /**
     * Build the synchronous response containing completion info and collected records.
     */
    private static JsonNode buildSyncResponse(CollectingJobEventListener collector,
                                              String jobType, boolean includeRecords) {
        var response = JsonHelper.getObjectMapper().createObjectNode();
        var exitCode = collector.getExitCode();
        response.put("status", exitCode == 0 ? "completed" : "failed");
        response.put("exitCode", exitCode);
        var stderr = collector.getStderr();
        if (stderr != null && !stderr.isBlank()) {
            response.put("stderr", stderr);
        }
        var stdout = collector.getStdout();
        if (stdout != null && !stdout.isBlank()) {
            response.put("stdout", stdout);
        }
        if (includeRecords) {
            var records = collector.getRecords();
            ArrayNode arr = response.putArray("records");
            records.forEach(arr::add);
            response.put("recordCount", records.size());
        }
        return response;
    }
}
