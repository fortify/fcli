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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Listener for async job lifecycle events. Implementations can push notifications,
 * cache records, or both. Used by {@link AsyncJobManager} to decouple job execution
 * from result delivery.
 *
 * <p>All methods are called from the background job thread unless noted otherwise.
 * Implementations must be thread-safe.</p>
 *
 * @author Ruud Senden
 */
public interface IJobEventListener {
    /** Called when a job starts execution on a worker thread. */
    void onJobStarted(String jobId, String description);

    /** Called for each record produced during execution. */
    void onRecord(String jobId, JsonNode record);

    /** Called for user-facing progress messages. */
    void onProgress(String jobId, String message);

    /** Called when a job completes (successfully or with error). */
    void onJobComplete(String jobId, int exitCode, String stderr, String stdout);

    /** No-op listener for use when no event handling is needed. */
    IJobEventListener NOOP = new IJobEventListener() {
        @Override public void onJobStarted(String jobId, String description) {}
        @Override public void onRecord(String jobId, JsonNode record) {}
        @Override public void onProgress(String jobId, String message) {}
        @Override public void onJobComplete(String jobId, int exitCode, String stderr, String stdout) {}
    };
}
