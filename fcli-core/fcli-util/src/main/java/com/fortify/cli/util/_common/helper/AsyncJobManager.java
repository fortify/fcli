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

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Manager for async background jobs. Provides background loading with progressive
 * record access for streaming jobs, and atomic result retrieval for non-streaming
 * jobs. Suitable for both MCP and RPC servers.
 *
 * @author Ruud Senden
 */
@Slf4j
public class AsyncJobManager {
    public static final long DEFAULT_TTL = 10 * 60 * 1000; // 10 minutes
    public static final int DEFAULT_MAX_ENTRIES = 5;
    public static final int DEFAULT_BG_THREADS = 2;

    /** Immutable configuration; use {@link Config#builder()} to construct. */
    @Value @Builder
    public static class Config {
        @Builder.Default int maxEntries = DEFAULT_MAX_ENTRIES;
        @Builder.Default long ttlMillis = DEFAULT_TTL;
        @Builder.Default int bgThreads = DEFAULT_BG_THREADS;
    }

    private final long ttl;
    private final Map<String, CompletedJobEntry> completedJobs;
    private final Map<String, InProgressEntry> inProgress = new ConcurrentHashMap<>();
    private final ExecutorService backgroundExecutor;

    public AsyncJobManager() {
        this(Config.builder().build());
    }

    public AsyncJobManager(Config config) {
        this.ttl = config.ttlMillis;
        this.completedJobs = new LinkedHashMap<>(config.maxEntries, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CompletedJobEntry> eldest) {
                return size() > config.maxEntries;
            }
        };
        this.backgroundExecutor = Executors.newFixedThreadPool(config.bgThreads, r -> {
            var t = new Thread(r, "fcli-async-job");
            t.setDaemon(true);
            return t;
        });
        log.info("Initialized AsyncJobManager: maxEntries={} ttl={}ms bgThreads={}",
            config.maxEntries, config.ttlMillis, config.bgThreads);
    }

    /**
     * Return a completed result for the given jobId if present and not expired, or start a
     * new background job via the given task. Returns {@code null} if already completed
     * (caller should look up the result via {@link #getCompleted}). Returns the
     * {@link InProgressEntry} if a job is running or was just started.
     */
    public InProgressEntry getOrStartBackground(String jobId, boolean refresh, IAsyncTask task) {
        var completed = getCompleted(jobId);
        if (!refresh && completed != null) {
            return null;
        }
        var existing = inProgress.get(jobId);
        if (existing != null && !existing.isExpired(ttl)) {
            return existing;
        }
        return startNewBackgroundJob(jobId, task);
    }

    /**
     * Start a background job and return a fresh {@code jobId} that can be used to
     * retrieve the result via {@link #getCompleted} or {@link #waitForCompletion}.
     */
    public String startBackground(IAsyncTask task) {
        var jobId = UUID.randomUUID().toString();
        getOrStartBackground(jobId, false, task);
        return jobId;
    }

    private InProgressEntry startNewBackgroundJob(String jobId, IAsyncTask task) {
        var entry = new InProgressEntry(jobId);
        inProgress.put(jobId, entry);
        var future = buildJobFuture(entry, task);
        future.whenComplete(createCompletionHandler(entry, jobId));
        entry.setFuture(future);
        log.debug("Started async job: jobId={}", jobId);
        return entry;
    }

    private CompletableFuture<FcliExecutionResult> buildJobFuture(InProgressEntry entry, IAsyncTask task) {
        return CompletableFuture.supplyAsync(() -> {
            var records = entry.getRecords();
            var result = task.run(record -> {
                if (!Thread.currentThread().isInterrupted()) {
                    records.add(record);
                }
            });
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            var fullResult = records.isEmpty() && result.getOut() != null && !result.getOut().isBlank()
                ? FcliExecutionResult.fromPlainText(result)
                : FcliExecutionResult.fromRecords(result, records);
            if (result.getExitCode() == 0) {
                putCompleted(entry.getJobId(), fullResult);
            }
            return fullResult;
        }, backgroundExecutor);
    }

    private BiConsumer<FcliExecutionResult, Throwable> createCompletionHandler(InProgressEntry entry, String jobId) {
        return (result, throwable) -> {
            entry.setCompleted(true);
            captureExecutionResult(entry, result, throwable);
            cleanupFailedJob(entry, jobId);
            log.debug("Async job completed: jobId={} exitCode={}", jobId, entry.getExitCode());
        };
    }

    private void captureExecutionResult(InProgressEntry entry, FcliExecutionResult result, Throwable throwable) {
        if (throwable != null) {
            entry.setExitCode(999);
            entry.setStderr(throwable.getMessage() != null ? throwable.getMessage() : "Async job failed");
        } else if (result != null) {
            entry.setExitCode(result.getExitCode());
            entry.setStderr(result.getStderr());
        } else {
            entry.setExitCode(999);
            entry.setStderr("Cancelled");
        }
    }

    private void cleanupFailedJob(InProgressEntry entry, String jobId) {
        if (entry.getExitCode() != 0) {
            inProgress.remove(jobId);
        }
    }

    private void putCompleted(String jobId, FcliExecutionResult result) {
        if (result == null) {
            return;
        }
        synchronized (completedJobs) {
            completedJobs.put(jobId, new CompletedJobEntry(result));
        }
        log.debug("Async job stored: jobId={} records={}", jobId, result.getRecords() != null ? result.getRecords().size() : 0);
    }

    /**
     * Return the completed result for {@code jobId} if present and not expired, or {@code null}.
     */
    public FcliExecutionResult getCompleted(String jobId) {
        synchronized (completedJobs) {
            var entry = completedJobs.get(jobId);
            return entry == null || entry.isExpired(ttl) ? null : entry.getResult();
        }
    }

    /**
     * Return the in-progress tracking entry for {@code jobId}, or {@code null} if not running.
     */
    public InProgressEntry getInProgress(String jobId) {
        return inProgress.get(jobId);
    }

    /**
     * Wait up to {@code maxWaitMs} for the job to complete and return its result.
     * Returns {@code null} if the job is still running when the timeout expires.
     * For failed jobs (non-zero exit code) a result is still returned.
     */
    public FcliExecutionResult waitForCompletion(String jobId, long maxWaitMs) {
        var entry = inProgress.get(jobId);
        if (entry == null) {
            return getCompleted(jobId);
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
            inProgress.remove(jobId);
            var completed = getCompleted(jobId);
            if (completed != null) return completed;
            // Job completed with non-zero exit code (not stored in completedJobs)
            return FcliExecutionResult.builder()
                .exitCode(entry.getExitCode())
                .stderr(entry.getStderr())
                .records(entry.getRecordsSnapshot())
                .build();
        }

        return null; // Still running / timed out
    }

    /**
     * Cancel a running job. Returns {@code true} if the job was found and cancelled.
     */
    public boolean cancel(String jobId) {
        var entry = inProgress.get(jobId);
        if (entry != null) {
            entry.cancel();
            inProgress.remove(jobId);
            log.debug("Cancelled async job: jobId={}", jobId);
            return true;
        }
        return false;
    }

    /**
     * Remove the result for a specific jobId (cancels if still running).
     * Returns {@code true} if anything was removed.
     */
    public boolean clear(String jobId) {
        boolean removed = false;
        synchronized (completedJobs) {
            removed = completedJobs.remove(jobId) != null;
        }
        var inProg = inProgress.remove(jobId);
        if (inProg != null) {
            inProg.cancel();
            removed = true;
        }
        return removed;
    }

    /**
     * Remove all completed results and cancel all running jobs.
     */
    public void clearAll() {
        synchronized (completedJobs) {
            completedJobs.clear();
        }
        inProgress.values().forEach(InProgressEntry::cancel);
        inProgress.clear();
        log.debug("Cleared all async jobs");
    }

    /**
     * Return current job counts.
     */
    public JobStats getStats() {
        int completed;
        synchronized (completedJobs) {
            completed = completedJobs.size();
        }
        return new JobStats(completed, inProgress.size());
    }

    /**
     * Shut down the background executor, waiting briefly for running jobs to finish.
     */
    public void shutdown() {
        backgroundExecutor.shutdown();
        try {
            backgroundExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        backgroundExecutor.shutdownNow();
        log.info("AsyncJobManager shutdown complete");
    }

    /**
     * Tracks a running async job: partial records, completion state, exit code, and stderr.
     */
    @Data
    public static final class InProgressEntry {
        private final String jobId;
        private final long created = System.currentTimeMillis();
        private final CopyOnWriteArrayList<JsonNode> records = new CopyOnWriteArrayList<>();
        private volatile CompletableFuture<FcliExecutionResult> future;
        private volatile boolean completed = false;
        private volatile int exitCode = 0;
        private volatile String stderr = "";

        public InProgressEntry(String jobId) {
            this.jobId = jobId;
        }

        public boolean isExpired(long ttl) {
            return System.currentTimeMillis() > created + ttl;
        }

        public void setFuture(CompletableFuture<FcliExecutionResult> f) {
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
    private static final class CompletedJobEntry {
        private final FcliExecutionResult result;
        private final long created = System.currentTimeMillis();

        public boolean isExpired(long ttl) {
            return System.currentTimeMillis() > created + ttl;
        }
    }

    @Data
    @RequiredArgsConstructor
    public static final class JobStats {
        private final int completedEntries;
        private final int runningEntries;
    }
}
