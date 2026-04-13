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
package com.fortify.cli.util.mcp_server.helper.mcp.runner;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.util._common.helper.AsyncJobManager;
import com.fortify.cli.util._common.helper.FcliExecutionResult;
import com.fortify.cli.util._common.helper.IAsyncTask;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;

/**
 * Thin adapter over {@link AsyncJobManager} for MCP tool use. Translates between
 * {@link FcliExecutionResult} and {@link MCPToolResult}, and registers background loading
 * futures with {@link MCPJobManager} for progress and cancellation support.
 */
public class MCPToolAsyncJobManager {
    private final AsyncJobManager delegate;
    private final MCPJobManager jobManager;
    private final Map<String, String> jobTokens = new ConcurrentHashMap<>();

    public MCPToolAsyncJobManager(MCPJobManager jobManager, AsyncJobManager delegate) {
        this.jobManager = jobManager;
        this.delegate = delegate;
    }

    /**
     * Return completed result if present and valid, or null.
     */
    public MCPToolResult getCached(String jobId) {
        var result = delegate.getCompleted(jobId);
        return result == null ? null : toMCPResult(result);
    }

    /**
     * Return completed result, or start/retrieve a background async job using the given task.
     * Returns null if already completed. Returns {@link InProgressEntry} if a
     * background job is in progress or was just started.
     */
    public InProgressEntry getOrStartBackground(String jobId, boolean refresh, IAsyncTask task) {
        var entry = delegate.getOrStartBackground(jobId, refresh, task);
        if (entry == null) return null;
        return trackEntry(jobId, entry);
    }

    private InProgressEntry trackEntry(String jobId, AsyncJobManager.InProgressEntry entry) {
        var jobToken = jobTokens.computeIfAbsent(jobId,
            k -> jobManager.trackFuture("async_job", entry.getFuture(),
                () -> entry.getRecords().size()));
        entry.getFuture().whenComplete((r, t) -> jobTokens.remove(jobId));
        return new InProgressEntry(entry, jobToken);
    }

    /** Cancel a background async job if running. */
    public void cancel(String jobId) {
        delegate.cancel(jobId);
    }

    /** Shutdown background executor gracefully. */
    public void shutdown() {
        delegate.shutdown();
    }

    private static MCPToolResult toMCPResult(FcliExecutionResult result) {
        return MCPToolResult.builder()
            .exitCode(result.getExitCode())
            .stderr(result.getStderr())
            .records(result.getRecords())
            .build();
    }

    /** Thin wrapper giving access to background collection state and its job tracking token. */
    public static final class InProgressEntry {
        private final AsyncJobManager.InProgressEntry delegate;
        private final String jobToken;

        InProgressEntry(AsyncJobManager.InProgressEntry delegate, String jobToken) {
            this.delegate = delegate;
            this.jobToken = jobToken;
        }

        public List<JsonNode> getRecords() { return delegate.getRecords(); }
        public boolean isCompleted() { return delegate.isCompleted(); }
        public int getExitCode() { return delegate.getExitCode(); }
        public String getStderr() { return delegate.getStderr(); }
        public String getJobToken() { return jobToken; }
    }
}
