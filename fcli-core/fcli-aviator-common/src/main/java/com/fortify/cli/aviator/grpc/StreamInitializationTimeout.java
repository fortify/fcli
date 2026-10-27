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

/** Bounds initialization without imposing a deadline on an established stream. */
final class StreamInitializationTimeout implements AutoCloseable {
    private enum State { WAITING, INITIALIZED, EXPIRED, CLOSED }

    private final AtomicReference<State> state = new AtomicReference<>(State.WAITING);
    private final ScheduledFuture<?> timeout;

    StreamInitializationTimeout(ScheduledExecutorService scheduler, Runnable onTimeout) {
        timeout = scheduler.schedule(() -> {
            if (state.compareAndSet(State.WAITING, State.EXPIRED)) {
                onTimeout.run();
            }
        }, 30, TimeUnit.SECONDS);
    }

    /** Returns false if termination won the race with the initialization response. */
    boolean initialized() {
        if (state.compareAndSet(State.WAITING, State.INITIALIZED)) {
            timeout.cancel(false);
        }
        return state.get() == State.INITIALIZED;
    }

    boolean isExpired() {
        return state.get() == State.EXPIRED;
    }

    @Override
    public void close() {
        state.compareAndSet(State.WAITING, State.CLOSED);
        timeout.cancel(false);
    }
}
