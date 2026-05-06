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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link IJobEventListener} that caches records as they arrive and provides
 * paged access. Used by MCP server (always) and optionally by RPC server
 * when {@code cache: true} is requested.
 *
 * @author Ruud Senden
 */
@Slf4j
public final class CachingJobEventListener implements IJobEventListener {
    private final Map<String, JobRecordCache> caches = new ConcurrentHashMap<>();
    private volatile ScheduledExecutorService evictionScheduler;

    @Override
    public void onJobStarted(String jobId, String description) {
        caches.put(jobId, new JobRecordCache(jobId));
        log.debug("Cache created for job: {}", jobId);
    }

    @Override
    public void onRecord(String jobId, JsonNode record) {
        var cache = caches.get(jobId);
        if (cache != null) {
            cache.records.add(record);
        }
    }

    @Override
    public void onProgress(String jobId, String message) {
        // Caching listener does not track progress messages
    }

    @Override
    public void onJobComplete(String jobId, int exitCode, String stderr, String stdout) {
        var cache = caches.get(jobId);
        if (cache != null) {
            cache.exitCode = exitCode;
            cache.stderr = stderr;
            cache.stdout = stdout;
            cache.completed = true;
            log.debug("Cache completed for job: {} records={} exitCode={}", jobId, cache.records.size(), exitCode);
        }
    }

    /**
     * Return a page of cached records for the given job.
     */
    public PageResult getPage(String jobId, int offset, int limit) {
        var cache = caches.get(jobId);
        if (cache == null) {
            return PageResult.notFound(jobId);
        }
        var snapshot = List.copyOf(cache.records);
        var totalLoaded = snapshot.size();
        var endIndex = Math.min(offset + limit, totalLoaded);
        var pageRecords = offset >= totalLoaded ? List.<JsonNode>of() : snapshot.subList(offset, endIndex);
        var hasMore = (offset + limit < totalLoaded) || !cache.completed;
        return PageResult.builder()
                .jobId(jobId)
                .status(cache.completed ? (cache.exitCode == 0 ? "complete" : "error") : "loading")
                .records(pageRecords)
                .offset(offset)
                .limit(limit)
                .loadedCount(totalLoaded)
                .hasMore(hasMore)
                .complete(cache.completed)
                .exitCode(cache.exitCode)
                .stderr(cache.stderr)
                .stdout(cache.stdout)
                .build();
    }

    /** Whether a cache exists and is complete for the given job. */
    public boolean isComplete(String jobId) {
        var cache = caches.get(jobId);
        return cache != null && cache.completed;
    }

    /** Number of records loaded so far for the given job. */
    public int getLoadedCount(String jobId) {
        var cache = caches.get(jobId);
        return cache != null ? cache.records.size() : 0;
    }

    /** Whether a cache exists for the given job. */
    public boolean hasCache(String jobId) {
        return caches.containsKey(jobId);
    }

    /** Remove the cache for a specific job. */
    public void remove(String jobId) {
        caches.remove(jobId);
    }

    /** Remove all caches. */
    public void clear() {
        caches.clear();
    }

    /**
     * Schedule cache removal for the given job after the specified TTL.
     * The eviction runs asynchronously on a daemon thread.
     */
    public void scheduleEviction(String jobId, Duration ttl) {
        getEvictionScheduler().schedule(() -> {
            remove(jobId);
            log.debug("Evicted cache for job {} after TTL of {}ms", jobId, ttl.toMillis());
        }, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** Shut down the eviction scheduler if active. */
    public void shutdown() {
        var scheduler = evictionScheduler;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private ScheduledExecutorService getEvictionScheduler() {
        var scheduler = evictionScheduler;
        if (scheduler != null) { return scheduler; }
        synchronized (this) {
            scheduler = evictionScheduler;
            if (scheduler == null) {
                scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    var t = new Thread(r, "cache-eviction");
                    t.setDaemon(true);
                    return t;
                });
                evictionScheduler = scheduler;
            }
            return scheduler;
        }
    }

    private static final class JobRecordCache {
        final String jobId;
        final CopyOnWriteArrayList<JsonNode> records = new CopyOnWriteArrayList<>();
        volatile boolean completed;
        volatile int exitCode;
        volatile String stderr;
        volatile String stdout;

        JobRecordCache(String jobId) {
            this.jobId = jobId;
        }
    }

    /** Result of a page query. */
    @Data @Builder
    public static final class PageResult {
        private final String jobId;
        private final String status;
        private final List<JsonNode> records;
        private final int offset;
        private final int limit;
        private final int loadedCount;
        private final boolean hasMore;
        private final boolean complete;
        private final int exitCode;
        private final String stderr;
        private final String stdout;

        public static PageResult notFound(String jobId) {
            return PageResult.builder()
                    .jobId(jobId)
                    .status("not_found")
                    .records(List.of())
                    .build();
        }
    }
}
