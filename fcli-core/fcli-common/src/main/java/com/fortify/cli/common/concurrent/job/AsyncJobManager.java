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
package com.fortify.cli.common.concurrent.job;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.fortify.cli.common.cli.util.FcliExecutionContext;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.cli.util.StdioHelper;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Pure execution engine for async background jobs. Manages a thread pool, tracks
 * in-progress jobs, and dispatches lifecycle events to an {@link IJobEventListener}.
 * No caching — record storage is delegated to listeners (e.g. {@link CachingJobEventListener}).
 *
 * <p>Suitable for both MCP and RPC servers.</p>
 *
 * @author Ruud Senden
 */
@Slf4j
public class AsyncJobManager {
    public static final int DEFAULT_BG_THREADS = 2;

    /** Immutable configuration; use {@link Config#builder()} to construct. */
    @Value @Builder
    public static class Config {
        @Builder.Default int bgThreads = DEFAULT_BG_THREADS;
    }

    /**
     * Descriptor for starting a background task.
     */
    @Value @Builder
    public static class TaskDescriptor {
        String jobId;
        IAsyncTask task;
        @Builder.Default IJobEventListener listener = IJobEventListener.NOOP;
        @Builder.Default String description = "";
        Supplier<FcliExecutionContext> executionContextSupplier;
    }

    private static final class ScopeState {
        private final Map<String, JobEntry> jobs = new ConcurrentHashMap<>();
    }

    private final ExecutorService backgroundExecutor;

    public AsyncJobManager() {
        this(Config.builder().build());
    }

    public AsyncJobManager(Config config) {
        this.backgroundExecutor = Executors.newFixedThreadPool(config.bgThreads, r -> {
            var t = new Thread(r, "fcli-async-job");
            t.setDaemon(true);
            return t;
        });
        log.info("Initialized AsyncJobManager: bgThreads={}", config.bgThreads);
    }

    /**
     * Start a background job described by the given {@link TaskDescriptor}.
     */
    public String startBackground(TaskDescriptor descriptor) {
        if ( descriptor == null ) {
            throw new IllegalArgumentException("TaskDescriptor must be specified");
        }
        var task = descriptor.getTask();
        if ( task == null ) {
            throw new IllegalArgumentException("TaskDescriptor.task must be specified");
        }
        var jobId = descriptor.getJobId() == null ? UUID.randomUUID().toString() : descriptor.getJobId();
        var listener = descriptor.getListener();
        var description = descriptor.getDescription() == null ? "" : descriptor.getDescription();
        var parentContext = FcliExecutionContextHolder.current();
        var executionContextSupplier = descriptor.getExecutionContextSupplier();
        var scopeState = getScopeState();
        var entry = new JobEntry(jobId, description);
        scopeState.jobs.put(jobId, entry);

        listener.onJobStarted(jobId, description);

        var future = CompletableFuture.runAsync(() -> {
            entry.thread = Thread.currentThread();
            try (var frame = FcliExecutionContextHolder.push(executionContextSupplier != null ? executionContextSupplier.get() : parentContext.createChild())) {
                // Register per-thread progress callback so that progress writer
                // messages are forwarded to the job event listener as notifications.
                // Masking is applied by StdioHelper before invoking the callback.
                StdioHelper.setProgressCallback(msg -> listener.onProgress(jobId, msg));
                try {
                    var result = task.run(record -> {
                        if (!Thread.currentThread().isInterrupted()) {
                            listener.onRecord(jobId, record);
                        }
                    });
                    if (!Thread.currentThread().isInterrupted()) {
                        int exitCode = result.getExitCode();
                        String stderr = result.getErr();
                        String stdout = result.getOut();
                        if (stdout != null && !stdout.isBlank()) {
                            entry.stdout = stdout;
                        }
                        entry.exitCode = exitCode;
                        entry.stderr = stderr;
                        listener.onJobComplete(jobId, exitCode, stderr, stdout);
                    }
                } catch (Exception e) {
                    log.error("Async job failed: jobId={}", jobId, e);
                    entry.exitCode = 999;
                    entry.stderr = e.getMessage() != null ? e.getMessage() : "Async job failed";
                    listener.onJobComplete(jobId, 999, entry.stderr, null);
                } finally {
                    StdioHelper.clearProgressCallback();
                    entry.completed = true;
                }
            }
        }, backgroundExecutor);

        entry.future = future;
        log.debug("Started async job: jobId={} description={}", jobId, description);
        return jobId;
    }

    /**
     * Cancel a running job. Returns {@code true} if the job was found and cancelled.
     */
    public boolean cancel(String jobId) {
        var entry = getScopeState().jobs.get(jobId);
        if (entry != null && !entry.completed) {
            var thread = entry.thread;
            if (thread != null) {
                thread.interrupt();
            }
            if (entry.future != null) {
                entry.future.cancel(true);
            }
            getScopeState().jobs.remove(jobId);
            log.debug("Cancelled async job: jobId={}", jobId);
            return true;
        }
        return false;
    }

    /** Whether a job is currently running (not completed). */
    public boolean isRunning(String jobId) {
        var entry = getScopeState().jobs.get(jobId);
        return entry != null && !entry.completed;
    }

    /** Return the future for a job, or null if not found. */
    public java.util.concurrent.CompletableFuture<Void> getFuture(String jobId) {
        var entry = getScopeState().jobs.get(jobId);
        return entry != null ? entry.future : null;
    }

    /** Return info about all tracked jobs. */
    public List<JobInfo> listJobs() {
        return getScopeState().jobs.values().stream()
                .map(e -> new JobInfo(e.jobId, e.description, e.completed, e.exitCode, e.created))
                .toList();
    }

    /** Return info about a single tracked job, or null if not found. */
    public JobInfo getJobInfo(String jobId) {
        var entry = getScopeState().jobs.get(jobId);
        return entry != null
                ? new JobInfo(entry.jobId, entry.description, entry.completed, entry.exitCode, entry.created)
                : null;
    }

    /** Remove a completed job from tracking. */
    public void removeJob(String jobId) {
        var entry = getScopeState().jobs.get(jobId);
        if (entry != null && entry.completed) {
            getScopeState().jobs.remove(jobId);
        }
    }

    /** Return the stdout captured for a completed job, or null. */
    public String getStdout(String jobId) {
        var entry = getScopeState().jobs.get(jobId);
        return entry != null ? entry.stdout : null;
    }

    private ScopeState getScopeState() {
        return FcliExecutionContextHolder.current().getIsolationScope().getOrCreateScopedState(ScopeState.class, ScopeState::new);
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

    /** Internal tracking for a running job. */
    private static final class JobEntry {
        final String jobId;
        final String description;
        final long created = System.currentTimeMillis();
        volatile CompletableFuture<Void> future;
        volatile Thread thread;
        volatile boolean completed;
        volatile int exitCode;
        volatile String stderr;
        volatile String stdout;

        JobEntry(String jobId, String description) {
            this.jobId = jobId;
            this.description = description;
        }
    }

    /** Snapshot of job metadata for listing. */
    @Data @RequiredArgsConstructor
    public static final class JobInfo {
        private final String jobId;
        private final String description;
        private final boolean completed;
        private final int exitCode;
        private final long createdMillis;
    }
}
