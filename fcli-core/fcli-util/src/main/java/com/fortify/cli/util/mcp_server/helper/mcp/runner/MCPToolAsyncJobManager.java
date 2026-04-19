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
import com.fortify.cli.util._common.helper.CachingJobEventListener;
import com.fortify.cli.util._common.helper.IAsyncTask;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;

/**
 * Thin adapter over {@link AsyncJobManager} for MCP tool use. Uses a
 * {@link CachingJobEventListener} to cache records, and registers background
 * futures with {@link MCPJobManager} for progress and cancellation support.
 */
public class MCPToolAsyncJobManager {
    private final AsyncJobManager delegate;
    private final MCPJobManager jobManager;
    private final CachingJobEventListener cachingListener = new CachingJobEventListener();
    private final Map<String, String> jobTokens = new ConcurrentHashMap<>();

    public MCPToolAsyncJobManager(MCPJobManager jobManager, AsyncJobManager delegate) {
        this.jobManager = jobManager;
        this.delegate = delegate;
    }

    /**
     * Return completed result if present and valid, or null.
     */
    public MCPToolResult getCached(String jobId) {
        if (!cachingListener.isComplete(jobId)) {
            return null;
        }
        var page = cachingListener.getPage(jobId, 0, Integer.MAX_VALUE);
        var builder = MCPToolResult.builder()
            .exitCode(page.getExitCode())
            .stderr(page.getStderr())
            .records(page.getRecords());
        if (page.getExitCode() != 0) {
            builder.error(page.getStderr())
                .errorGuidance("The command failed; records may be incomplete.");
        }
        return builder.build();
    }

    /**
     * Return completed result, or start/retrieve a background async job using the given task.
     * Returns null if already completed. Returns {@link InProgressEntry} if a
     * background job is in progress or was just started.
     */
    public InProgressEntry getOrStartBackground(String jobId, boolean refresh, IAsyncTask task) {
        if (!refresh && cachingListener.isComplete(jobId)) {
            return null;
        }
        if (refresh) {
            cachingListener.remove(jobId);
        }
        if (delegate.isRunning(jobId)) {
            return new InProgressEntry(jobId, cachingListener, jobTokens.get(jobId));
        }
        // Start new background job with the semantic jobId
        delegate.startBackground(jobId, task, cachingListener, "mcp:" + jobId);
        var future = delegate.getFuture(jobId);
        if (future != null) {
            var jobToken = jobManager.trackFuture("async_job", future,
                    () -> cachingListener.getLoadedCount(jobId));
            jobTokens.put(jobId, jobToken);
            future.whenComplete((r, t) -> jobTokens.remove(jobId));
        }
        return new InProgressEntry(jobId, cachingListener, jobTokens.get(jobId));
    }

    /** Cancel a background async job if running. */
    public void cancel(String jobId) {
        delegate.cancel(jobId);
    }

    /** Shutdown background executor gracefully. */
    public void shutdown() {
        delegate.shutdown();
    }

    /** Thin wrapper giving access to background collection state via CachingJobEventListener. */
    public static final class InProgressEntry {
        private final String jobId;
        private final CachingJobEventListener cachingListener;
        private final String jobToken;

        InProgressEntry(String jobId, CachingJobEventListener cachingListener, String jobToken) {
            this.jobId = jobId;
            this.cachingListener = cachingListener;
            this.jobToken = jobToken;
        }

        public List<JsonNode> getRecords() {
            var page = cachingListener.getPage(jobId, 0, Integer.MAX_VALUE);
            return page.getRecords();
        }

        public int getLoadedCount() {
            return cachingListener.getLoadedCount(jobId);
        }

        public boolean isCompleted() { return cachingListener.isComplete(jobId); }

        public int getExitCode() {
            var page = cachingListener.getPage(jobId, 0, 0);
            return page.getExitCode();
        }

        public String getStderr() {
            var page = cachingListener.getPage(jobId, 0, 0);
            return page.getStderr();
        }

        public String getJobToken() { return jobToken; }
    }
}
