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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;

/**
 * {@link IJobEventListener} that collects all records in memory and signals
 * completion via a {@link CountDownLatch}. Used when the caller wants to wait
 * for the job to finish and return all results inline (synchronous wait mode).
 *
 * <p>Can optionally delegate to another listener (e.g., caching or push) so
 * that the wait behavior composes with existing listener configuration.</p>
 *
 * @author Ruud Senden
 */
public final class CollectingJobEventListener implements IJobEventListener {
    private final IJobEventListener delegate;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final List<JsonNode> records = new CopyOnWriteArrayList<>();
    @Getter private volatile int exitCode;
    @Getter private volatile String stderr;
    @Getter private volatile String stdout;

    public CollectingJobEventListener(IJobEventListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onJobStarted(String jobId, String description) {
        delegate.onJobStarted(jobId, description);
    }

    @Override
    public void onRecord(String jobId, JsonNode record) {
        records.add(record);
        delegate.onRecord(jobId, record);
    }

    @Override
    public void onProgress(String jobId, String message) {
        delegate.onProgress(jobId, message);
    }

    @Override
    public void onJobComplete(String jobId, int exitCode, String stderr, String stdout) {
        this.exitCode = exitCode;
        this.stderr = stderr;
        this.stdout = stdout;
        delegate.onJobComplete(jobId, exitCode, stderr, stdout);
        latch.countDown();
    }

    /**
     * Wait for job completion up to the given timeout.
     * @return {@code true} if the job completed within the timeout, {@code false} otherwise
     */
    public boolean await(Duration timeout) throws InterruptedException {
        return latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Wait indefinitely for job completion.
     */
    public void await() throws InterruptedException {
        latch.await();
    }

    /** Return a snapshot of all collected records. */
    public List<JsonNode> getRecords() {
        return List.copyOf(records);
    }
}
