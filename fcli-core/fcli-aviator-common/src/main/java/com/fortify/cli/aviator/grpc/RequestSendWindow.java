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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Client-side sliding window for stream requests. Keeps at most {@code windowSize} requests
 * in flight on a stream; a slot is only freed when a request reaches a terminal state
 * (response or error), so the number of outstanding requests never depends on how many
 * credits the server grants.
 */
final class RequestSendWindow<T> {
    private final int windowSize;
    private final Function<T, String> requestIdOf;
    private final Deque<T> queue = new ArrayDeque<>();
    private final Set<String> inFlight = new HashSet<>();

    RequestSendWindow(int windowSize, Function<T, String> requestIdOf) {
        if (windowSize <= 0) throw new IllegalArgumentException("windowSize must be positive");
        this.windowSize = windowSize;
        this.requestIdOf = requestIdOf;
    }

    synchronized void enqueue(List<T> messages) {
        queue.addAll(messages);
    }

    /** Takes as many queued messages as the free window allows and marks them in flight. */
    synchronized List<T> drainSendable() {
        List<T> sendable = new ArrayList<>();
        while (inFlight.size() < windowSize && !queue.isEmpty()) {
            T message = queue.pollFirst();
            inFlight.add(requestIdOf.apply(message));
            sendable.add(message);
        }
        return sendable;
    }

    /** Frees the slot held by a request. Returns false if the request was not in flight. */
    synchronized boolean release(String requestId) {
        return inFlight.remove(requestId);
    }

    synchronized void clear() {
        queue.clear();
        inFlight.clear();
    }

    synchronized int inFlightCount() {
        return inFlight.size();
    }

    synchronized int queuedCount() {
        return queue.size();
    }
}
