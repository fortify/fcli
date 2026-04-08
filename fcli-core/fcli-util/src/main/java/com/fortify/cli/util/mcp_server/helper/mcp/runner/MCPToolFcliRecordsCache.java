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
import com.fortify.cli.util._common.helper.FcliExecutionResult;
import com.fortify.cli.util._common.helper.FcliRecordsCache;
import com.fortify.cli.util._common.helper.IRecordProducer;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;

/**
 * Thin adapter over {@link FcliRecordsCache} for MCP tool use. Translates between
 * {@link FcliToolResult} and {@link MCPToolResult}, and registers background loading
 * futures with {@link MCPJobManager} for progress and cancellation support.
 */
public class MCPToolFcliRecordsCache {
    private final FcliRecordsCache delegate;
    private final MCPJobManager jobManager;
    private final Map<String, String> jobTokens = new ConcurrentHashMap<>();

    public MCPToolFcliRecordsCache(MCPJobManager jobManager) {
        this.jobManager = jobManager;
        this.delegate = new FcliRecordsCache();
    }

    /**
     * Return cached full result if present and valid, or null.
     */
    public MCPToolResult getCached(String fullCmd) {
        var result = delegate.getCached(fullCmd);
        return result == null ? null : toMCPResult(result);
    }

    /**
     * Return cached result, or start/retrieve a background collection using the given producer.
     * Returns null if already cached. Returns {@link InProgressEntry} if a
     * background collection is in progress or was just started.
     */
    public InProgressEntry getOrStartBackground(String cacheKey, boolean refresh, IRecordProducer producer) {
        var entry = delegate.getOrStartBackground(cacheKey, refresh, producer);
        if (entry == null) return null;
        return trackEntry(cacheKey, entry);
    }

    private InProgressEntry trackEntry(String cacheKey, FcliRecordsCache.InProgressEntry entry) {
        var jobToken = jobTokens.computeIfAbsent(cacheKey,
            k -> jobManager.trackFuture("cache_loader", entry.getFuture(),
                () -> entry.getRecords().size()));
        entry.getFuture().whenComplete((r, t) -> jobTokens.remove(cacheKey));
        return new InProgressEntry(entry, jobToken);
    }

    /** Cancel a background collection if running. */
    public void cancel(String fullCmd) {
        delegate.cancel(fullCmd);
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
        private final FcliRecordsCache.InProgressEntry delegate;
        private final String jobToken;

        InProgressEntry(FcliRecordsCache.InProgressEntry delegate, String jobToken) {
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
