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
package com.fortify.cli.util._common.helper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cache for fcli record-collecting operations. Provides background loading with
 * progressive record access, suitable for both MCP and RPC servers.
 * 
 * Features:
 * - LRU cache with configurable size and TTL
 * - Background async loading with partial result access
 * - Cancel support for long-running collections
 * - Thread-safe concurrent access
 * - Support for session options through option resolver
 * 
 * @author Ruud Senden
 */
@Slf4j
public class FcliRecordsCache {
    private static final long DEFAULT_TTL = 10 * 60 * 1000; // 10 minutes
    private static final int DEFAULT_MAX_ENTRIES = 5;
    private static final int DEFAULT_BG_THREADS = 2;
    
    private final long ttl;
    private final int maxEntries;
    private final Map<String, CacheEntry> cache;
    private final Map<String, InProgressEntry> inProgress = new ConcurrentHashMap<>();
    private final ExecutorService backgroundExecutor;
    private Function<String, Map<String, String>> optionResolver;
    
    public FcliRecordsCache() {
        this(DEFAULT_MAX_ENTRIES, DEFAULT_TTL, DEFAULT_BG_THREADS);
    }
    
    public FcliRecordsCache(int maxEntries, long ttlMillis, int bgThreads) {
        this.ttl = ttlMillis;
        this.maxEntries = maxEntries;
        // Use access-ordered LinkedHashMap for LRU behavior
        this.cache = new LinkedHashMap<>(maxEntries, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > maxEntries;
            }
        };
        this.backgroundExecutor = Executors.newFixedThreadPool(bgThreads, r -> {
            var t = new Thread(r, "fcli-cache-loader");
            t.setDaemon(true);
            return t;
        });
        log.info("Initialized FcliRecordsCache: maxEntries={} ttl={}ms bgThreads={}", maxEntries, ttlMillis, bgThreads);
    }
    
    /**
     * Set a function to resolve default options for commands (e.g., session options).
     */
    public void setOptionResolver(Function<String, Map<String, String>> resolver) {
        this.optionResolver = resolver;
    }
    
    /**
     * Get cached result, or start background collection if not cached.
     * Returns null if result is already cached (caller should use getCached).
     * Returns InProgressEntry if background collection started/exists.
     */
    public InProgressEntry getOrStartBackground(String cacheKey, boolean refresh, String command) {
        var cached = getCached(cacheKey);
        if (!refresh && cached != null) {
            return null;  // Already cached
        }
        
        var existing = inProgress.get(cacheKey);
        if (existing != null && !existing.isExpired(ttl)) {
            return existing;  // Already loading
        }
        
        return startNewBackgroundCollection(cacheKey, command);
    }
    
    /**
     * Start a background collection and return immediately with the cacheKey.
     */
    public String startBackgroundCollection(String command) {
        var cacheKey = UUID.randomUUID().toString();
        startNewBackgroundCollection(cacheKey, command);
        return cacheKey;
    }
    
    private InProgressEntry startNewBackgroundCollection(String cacheKey, String command) {
        var entry = new InProgressEntry(cacheKey, command);
        inProgress.put(cacheKey, entry);
        
        var future = buildCollectionFuture(entry, command);
        future.whenComplete(createCompletionHandler(entry, cacheKey));
        
        entry.setFuture(future);
        log.debug("Started background collection: cacheKey={} command={}", cacheKey, command);
        
        return entry;
    }
    
    private CompletableFuture<FcliToolResult> buildCollectionFuture(InProgressEntry entry, String command) {
        // Resolve options before starting async execution
        var defaultOptions = optionResolver != null ? optionResolver.apply(command) : null;
        
        return CompletableFuture.supplyAsync(() -> {
            var records = entry.getRecords();
            var result = FcliRunnerHelper.collectRecords(command, record -> {
                if (!Thread.currentThread().isInterrupted()) {
                    records.add(record);
                }
            }, defaultOptions);
            
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            
            var fullResult = FcliToolResult.fromRecords(result, records);
            if (result.getExitCode() == 0) {
                put(entry.getCacheKey(), fullResult);
            }
            return fullResult;
        }, backgroundExecutor);
    }
    
    private BiConsumer<FcliToolResult, Throwable> createCompletionHandler(InProgressEntry entry, String cacheKey) {
        return (result, throwable) -> {
            entry.setCompleted(true);
            captureExecutionResult(entry, result, throwable);
            cleanupFailedCollection(entry, cacheKey);
            log.debug("Background collection completed: cacheKey={} exitCode={}", cacheKey, entry.getExitCode());
        };
    }
    
    private void captureExecutionResult(InProgressEntry entry, FcliToolResult result, Throwable throwable) {
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
    
    private void cleanupFailedCollection(InProgressEntry entry, String cacheKey) {
        if (entry.getExitCode() != 0) {
            inProgress.remove(cacheKey);
        }
    }
    
    /**
     * Store a result in the cache.
     */
    public void put(String cacheKey, FcliToolResult result) {
        if (result == null) {
            return;
        }
        synchronized (cache) {
            cache.put(cacheKey, new CacheEntry(result));
        }
        log.debug("Cached result: cacheKey={} records={}", cacheKey, result.getRecords() != null ? result.getRecords().size() : 0);
    }
    
    /**
     * Get a cached result if present and not expired.
     */
    public FcliToolResult getCached(String cacheKey) {
        synchronized (cache) {
            var entry = cache.get(cacheKey);
            return entry == null || entry.isExpired(ttl) ? null : entry.getFullResult();
        }
    }
    
    /**
     * Get an in-progress entry if exists.
     */
    public InProgressEntry getInProgress(String cacheKey) {
        return inProgress.get(cacheKey);
    }
    
    /**
     * Wait for collection to complete (up to maxWaitMs) and return the result.
     */
    public FcliToolResult waitForCompletion(String cacheKey, long maxWaitMs) {
        var entry = inProgress.get(cacheKey);
        if (entry == null) {
            return getCached(cacheKey);
        }
        
        long start = System.currentTimeMillis();
        while (!entry.isCompleted() && System.currentTimeMillis() - start < maxWaitMs) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (entry.isCompleted()) {
            inProgress.remove(cacheKey);
            return getCached(cacheKey);
        }
        
        return null;  // Still in progress
    }
    
    /**
     * Cancel a background collection.
     */
    public boolean cancel(String cacheKey) {
        var entry = inProgress.get(cacheKey);
        if (entry != null) {
            entry.cancel();
            inProgress.remove(cacheKey);
            log.debug("Cancelled collection: cacheKey={}", cacheKey);
            return true;
        }
        return false;
    }
    
    /**
     * Clear a specific cache entry.
     */
    public boolean clear(String cacheKey) {
        boolean removed = false;
        synchronized (cache) {
            removed = cache.remove(cacheKey) != null;
        }
        var inProg = inProgress.remove(cacheKey);
        if (inProg != null) {
            inProg.cancel();
            removed = true;
        }
        return removed;
    }
    
    /**
     * Clear all cache entries.
     */
    public void clearAll() {
        synchronized (cache) {
            cache.clear();
        }
        inProgress.values().forEach(InProgressEntry::cancel);
        inProgress.clear();
        log.debug("Cleared all cache entries");
    }
    
    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        int cached;
        synchronized (cache) {
            cached = cache.size();
        }
        return new CacheStats(cached, inProgress.size());
    }
    
    /**
     * Shutdown the cache and background executor.
     */
    public void shutdown() {
        backgroundExecutor.shutdown();
        try {
            backgroundExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        backgroundExecutor.shutdownNow();
        log.info("FcliRecordsCache shutdown complete");
    }
    
    /**
     * In-progress tracking entry giving access to partial records list.
     */
    @Data
    public static final class InProgressEntry {
        private final String cacheKey;
        private final String command;
        private final long created = System.currentTimeMillis();
        private final CopyOnWriteArrayList<JsonNode> records = new CopyOnWriteArrayList<>();
        private volatile CompletableFuture<FcliToolResult> future;
        private volatile boolean completed = false;
        private volatile int exitCode = 0;
        private volatile String stderr = "";
        
        public InProgressEntry(String cacheKey, String command) {
            this.cacheKey = cacheKey;
            this.command = command;
        }
        
        public boolean isExpired(long ttl) {
            return System.currentTimeMillis() > created + ttl;
        }
        
        public void setFuture(CompletableFuture<FcliToolResult> f) {
            this.future = f;
        }
        
        public void cancel() {
            if (future != null) {
                future.cancel(true);
            }
        }
        
        public int getLoadedCount() {
            return records.size();
        }
        
        public List<JsonNode> getRecordsSnapshot() {
            return List.copyOf(records);
        }
    }
    
    @Data
    @RequiredArgsConstructor
    private static final class CacheEntry {
        private final FcliToolResult fullResult;
        private final long created = System.currentTimeMillis();
        
        public boolean isExpired(long ttl) {
            return System.currentTimeMillis() > created + ttl;
        }
    }
    
    @Data
    @RequiredArgsConstructor
    public static final class CacheStats {
        private final int cachedEntries;
        private final int inProgressEntries;
    }
}
