/*
 * Copyright 2021-2025 Open Text.
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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.LRUMap;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Cache for fcli record-collecting MCP tools. A single instance is owned by {@link MCPJobManager}
 * and constructed with a mandatory {@link MCPJobManager} reference so background collection jobs
 * are always tracked (job manager integration is no longer optional).
 */
public class MCPToolFcliRecordsCache {
    private static final long TTL = 10*60*1000; // 10 minutes in milliseconds
    private static final int MAX_CACHE_ENTRIES = 5; // Keep small; large sets expensive
    private static final int BG_THREADS = 2; // Single-threaded background collection
    private final LRUMap<String, CacheEntry> cache = new LRUMap<>(0, MAX_CACHE_ENTRIES);
    private final Map<String, InProgressEntry> inProgress = new ConcurrentHashMap<>();
    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(BG_THREADS, r->{
        var t = new Thread(r, "fcli-mcp-cache-loader");
        t.setDaemon(true); // Allow JVM exit
        return t;
    });
    private final MCPJobManager jobManager; // Required integration

    public MCPToolFcliRecordsCache(MCPJobManager jobManager) {
        this.jobManager = jobManager;
    }

    /**
     * Return cached full result if present and valid (respecting refresh). Otherwise
     * start (or reuse) an asynchronous background collection and return the in-progress
     * entry for partial access.
     */
    public final InProgressEntry getOrStartBackground(String fullCmd, boolean refresh, CommandSpec spec) {
        var cached = getCached(fullCmd);
        if (!refresh && cached != null) {
            return null;
        }
        
        var existing = inProgress.get(fullCmd);
        if (existing != null && !existing.isExpired()) {
            return existing;
        }
        
        return startNewBackgroundCollection(fullCmd, spec);
    }
    
    private InProgressEntry startNewBackgroundCollection(String fullCmd, CommandSpec spec) {
        var entry = new InProgressEntry(fullCmd);
        inProgress.put(fullCmd, entry);
        
        var future = buildCollectionFuture(entry, fullCmd, spec);
        future.whenComplete(createCompletionHandler(entry, fullCmd));
        
        entry.setFuture(future);
        entry.setJobToken(trackCollectionJob(entry, future));
        
        return entry;
    }
    
    private CompletableFuture<MCPToolResult> buildCollectionFuture(
            InProgressEntry entry, String fullCmd, CommandSpec spec) {
        return CompletableFuture.supplyAsync(() -> {
            var records = entry.getRecords();
            var result = MCPToolFcliRunnerHelper.collectRecords(fullCmd, record -> {
                if (!Thread.currentThread().isInterrupted()) {
                    records.add(record);
                }
            }, spec);
            
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            
            var fullResult = MCPToolResult.fromRecords(result, records);
            if (result.getExitCode() == 0) {
                put(fullCmd, fullResult);
            }
            return fullResult;
        }, backgroundExecutor);
    }
    
    private BiConsumer<MCPToolResult, Throwable> createCompletionHandler(
            InProgressEntry entry, String fullCmd) {
        return (result, throwable) -> {
            entry.setCompleted(true);
            captureExecutionResult(entry, result, throwable);
            cleanupFailedCollection(entry, fullCmd);
        };
    }
    
    private void captureExecutionResult(InProgressEntry entry, MCPToolResult result, Throwable throwable) {
        if (throwable != null) {
            entry.setExitCode(999);
            entry.setStderr(throwable.getMessage() != null ? throwable.getMessage() : "Background collection failed");
        } else if (result != null) {
            entry.setExitCode(result.getExitCode());
            entry.setStderr(result.getStderr());
        } else {
            entry.setExitCode(999);
            entry.setStderr("Cancelled");
        }
    }
    
    private void cleanupFailedCollection(InProgressEntry entry, String fullCmd) {
        if (entry.getExitCode() != 0) {
            inProgress.remove(fullCmd);
        }
    }
    
    private String trackCollectionJob(InProgressEntry entry, CompletableFuture<MCPToolResult> future) {
        return jobManager.trackFuture("cache_loader", future, () -> entry.getRecords().size());
    }
    
    public final void put(String fullCmd, MCPToolResult records) {
        if ( records==null ) {
            return;
        }
        synchronized(cache) {
            cache.put(fullCmd, new CacheEntry(records));
        }
    }
    
    public final MCPToolResult getCached(String fullCmd) {
        synchronized(cache) {
            var entry = cache.get(fullCmd);
            return entry==null || entry.isExpired() ? null : entry.getFullResult();
        }
    }

    /** Cancel a background collection if running. */
    public final void cancel(String fullCmd) {
        var inProg = inProgress.get(fullCmd);
        if ( inProg!=null ) {
            inProg.cancel();
        }
    }

    /** Shutdown background executor gracefully. */
    public final void shutdown() {
        backgroundExecutor.shutdown();
        try {
            backgroundExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
        }
        backgroundExecutor.shutdownNow();
    }

    /** In-progress tracking entry giving access to partial records list. */
    @Data
    public static final class InProgressEntry {
        private final String cmd;
        private final long created = System.currentTimeMillis();
        private final CopyOnWriteArrayList<JsonNode> records = new CopyOnWriteArrayList<>();
        private volatile CompletableFuture<MCPToolResult> future;
        private volatile boolean completed = false;
        private volatile int exitCode = 0; // Interim
        private volatile String stderr = ""; // Interim
        private volatile String jobToken; // Optional job token for tracking
        
        InProgressEntry(String cmd) {
            this.cmd = cmd;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > created + TTL;
        }
        
        void setFuture(CompletableFuture<MCPToolResult> f) {
            this.future = f;
        }
        
        void cancel() {
            if ( future!=null ) {
                future.cancel(true);
            }
        }
        
        void setJobToken(String jt) {
            this.jobToken = jt;
        }
    }
    
    @Data
    @RequiredArgsConstructor
    private static final class CacheEntry {
        private final MCPToolResult fullResult;
        private final long created = System.currentTimeMillis();
        
        public final boolean isExpired() {
            return System.currentTimeMillis() > created + TTL;
        }
    }
}
