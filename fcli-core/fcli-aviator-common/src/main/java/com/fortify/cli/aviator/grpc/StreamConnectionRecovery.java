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
package com.fortify.cli.aviator.grpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import com.fortify.cli.aviator.util.Constants;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Connection-only policy shared by DAST and correlation. Callers serialize access on their lifecycle
 * lock and retain ownership of replay, request-level retries, transport cancellation and result contracts.
 */
final class StreamConnectionRecovery {
    private final ScheduledExecutorService scheduler;
    private final Object lifecycleLock;
    private final LongSupplier nanoTime;
    private final Runnable expired;
    private Object episode;
    private long deadline;
    private int attempts;
    private ScheduledFuture<?> timer;

    StreamConnectionRecovery(ScheduledExecutorService scheduler, Object lifecycleLock, LongSupplier nanoTime, Runnable expired) {
        this.scheduler = scheduler;
        this.lifecycleLock = lifecycleLock;
        this.nanoTime = nanoTime;
        this.expired = expired;
    }

    boolean active() { return episode != null; }

    void begin() {
        if (active()) return;
        Object token = new Object();
        episode = token;
        attempts = 0;
        deadline = nanoTime.getAsLong() + TimeUnit.SECONDS.toNanos(150);
        Runnable timeout = () -> {
            synchronized (lifecycleLock) {
                if (episode == token) expired.run();
            }
        };
        if (scheduler == null) CompletableFuture.delayedExecutor(150, TimeUnit.SECONDS).execute(timeout);
        else timer = scheduler.schedule(timeout, 150, TimeUnit.SECONDS);
    }

    boolean expireIfOverdue() {
        if (!active() || nanoTime.getAsLong() - deadline < 0) return false;
        expired.run();
        return true;
    }

    long nextDelayMs() {
        return retryDelayMs(++attempts);
    }

    void end() {
        episode = null;
        if (timer != null) timer.cancel(false);
        timer = null;
        attempts = 0;
    }

    static long retryDelayMs(int attempt) {
        long delay = Constants.STREAM_RETRY_BASE_DELAY_MS * (1L << Math.min(Math.max(0, attempt - 1), 10));
        return Math.min(delay, Constants.STREAM_RETRY_MAX_DELAY_MS) + ThreadLocalRandom.current().nextLong(1000);
    }

    static boolean isRetryable(Throwable error) {
        if (!(error instanceof StatusRuntimeException)) return false;
        Status status = Status.fromThrowable(error);
        String description = status.getDescription();
        return status.getCode() == Status.Code.UNAVAILABLE
            || status.getCode() == Status.Code.INTERNAL && description != null
                && (description.contains("RST_STREAM") || description.contains("PROTOCOL_ERROR"));
    }
}
