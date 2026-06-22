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
package com.fortify.cli.util.rpc_server.helper;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.concurrent.job.IJobEventListener;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link IJobEventListener} that pushes JSON-RPC 2.0 notifications to the client
 * via the {@link RPCServer.RPCOutputWriter}. Records are batched (up to
 * {@code BATCH_SIZE} records or {@code BATCH_DELAY_MS} elapsed) to reduce
 * the number of individual notification messages.
 *
 * @author Ruud Senden
 */
@Slf4j
final class RPCPushJobEventListener implements IJobEventListener {
    private static final int BATCH_SIZE = 50;
    private static final long BATCH_DELAY_MS = 200;

    private final RPCServer.RPCOutputWriter outputWriter;

    // Per-job batching state — guarded by synchronizing on the batch list itself.
    // Simple approach: each job gets its own batch buffer. For the common case of
    // one or two concurrent jobs this is efficient and straightforward.
    private final java.util.concurrent.ConcurrentHashMap<String, BatchBuffer> batches =
            new java.util.concurrent.ConcurrentHashMap<>();

    RPCPushJobEventListener(RPCServer.RPCOutputWriter outputWriter) {
        this.outputWriter = outputWriter;
    }

    @Override
    public void onJobStarted(String jobId, String description) {
        batches.put(jobId, new BatchBuffer());
        outputWriter.send(RPCNotification.jobStarted(jobId, description));
        log.debug("Pushed job.started notification: jobId={}", jobId);
    }

    @Override
    public void onRecord(String jobId, JsonNode record) {
        var buffer = batches.get(jobId);
        if (buffer == null) {
            // Job already completed or not tracked; send immediately
            outputWriter.send(RPCNotification.jobRecords(jobId, List.of(record)));
            return;
        }
        List<JsonNode> toSend = null;
        synchronized (buffer) {
            buffer.records.add(record);
            if (buffer.records.size() >= BATCH_SIZE ||
                System.currentTimeMillis() - buffer.lastFlush >= BATCH_DELAY_MS) {
                toSend = new ArrayList<>(buffer.records);
                buffer.records.clear();
                buffer.lastFlush = System.currentTimeMillis();
            }
        }
        if (toSend != null) {
            outputWriter.send(RPCNotification.jobRecords(jobId, toSend));
            log.trace("Pushed {} records for job {}", toSend.size(), jobId);
        }
    }

    @Override
    public void onProgress(String jobId, String message) {
        outputWriter.send(RPCNotification.jobProgress(jobId, message));
    }

    @Override
    public void onJobComplete(String jobId, int exitCode, String stderr, String stdout) {
        // Flush any remaining buffered records
        var buffer = batches.remove(jobId);
        if (buffer != null) {
            List<JsonNode> remaining;
            synchronized (buffer) {
                remaining = new ArrayList<>(buffer.records);
                buffer.records.clear();
            }
            if (!remaining.isEmpty()) {
                outputWriter.send(RPCNotification.jobRecords(jobId, remaining));
                log.trace("Flushed {} remaining records for job {}", remaining.size(), jobId);
            }
        }
        outputWriter.send(RPCNotification.jobComplete(jobId, exitCode, stderr, stdout));
        log.debug("Pushed job.complete notification: jobId={} exitCode={}", jobId, exitCode);
    }

    private static final class BatchBuffer {
        final List<JsonNode> records = new ArrayList<>();
        long lastFlush = System.currentTimeMillis();
    }
}
