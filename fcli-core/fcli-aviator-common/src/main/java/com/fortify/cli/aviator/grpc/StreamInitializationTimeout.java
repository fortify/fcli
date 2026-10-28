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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;

/**
 * Bounds the wait for one stream attempt's initialization response. The expiry action must only
 * cancel the RPC; the processor's own onError path then runs its normal terminal handling.
 *
 * <p>TODO: Nothing bounds silence after initialization (pongs are only logged). Add an idle/no-progress
 * timeout for the SAST, DAST and correlation streams.
 */
final class StreamInitializationTimeout implements AutoCloseable {
    private static final long TIMEOUT_SECONDS = 30;

    private enum State { IDLE, WAITING, INITIALIZED, EXPIRED, CLOSED }

    private final ScheduledExecutorService scheduler;
    private final Runnable cancelRpc;
    private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);
    private volatile ScheduledFuture<?> timeout;

    StreamInitializationTimeout(ScheduledExecutorService scheduler, Runnable cancelRpc) {
        this.scheduler = scheduler;
        this.cancelRpc = cancelRpc;
    }

    static AviatorSimpleException expiredError(String operation) {
        return new AviatorSimpleException(operation
            + " initialization timed out. Check the Aviator URL, proxy settings, and server availability.");
    }

    /** Starts the countdown after the init request is queued. Does nothing without a scheduler. */
    void arm() {
        if (scheduler == null || !state.compareAndSet(State.IDLE, State.WAITING)) return;
        timeout = scheduler.schedule(this::expire, TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (state.get() != State.WAITING) timeout.cancel(false);
    }

    /** Returns false if the timeout or a close won the race with the initialization response. */
    boolean initialized() {
        if (state.compareAndSet(State.WAITING, State.INITIALIZED)
                || state.compareAndSet(State.IDLE, State.INITIALIZED)) {
            cancelTimer();
        }
        return state.get() == State.INITIALIZED;
    }

    boolean isExpired() {
        return state.get() == State.EXPIRED;
    }

    @Override
    public void close() {
        if (state.compareAndSet(State.WAITING, State.CLOSED) || state.compareAndSet(State.IDLE, State.CLOSED)) {
            cancelTimer();
        }
    }

    private void expire() {
        if (state.compareAndSet(State.WAITING, State.EXPIRED)) {
            cancelRpc.run();
        }
    }

    private void cancelTimer() {
        var scheduled = timeout;
        if (scheduled != null) scheduled.cancel(false);
    }
}