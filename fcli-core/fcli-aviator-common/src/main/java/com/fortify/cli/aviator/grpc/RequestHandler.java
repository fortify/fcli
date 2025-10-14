/*
 * Copyright 2021-2025 Open Text.
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

import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.StreamObserver;
import lombok.Getter;

/**
 * Thread-safe request handler for gRPC client streams.
 * Ensures all stream operations are synchronized and prevents concurrent modifications.
 */
public class RequestHandler<T> {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private volatile StreamObserver<T> requestObserver;
    private final String streamId;
    @Getter
    private final Deque<T> requestQueue;
    private final ReentrantLock sendLock;
    private final AtomicBoolean isCompleted;
    private final AtomicBoolean isInitialized;
    private final AtomicInteger pendingRequests;

    // Metrics
    private long totalSent = 0;
    private long totalErrors = 0;

    public RequestHandler(String streamId) {
        this.streamId = streamId;
        this.requestQueue = new ConcurrentLinkedDeque<>();
        this.sendLock = new ReentrantLock();
        this.isCompleted = new AtomicBoolean(false);
        this.isInitialized = new AtomicBoolean(false);
        this.pendingRequests = new AtomicInteger(0);
    }

    /**
     * Initialize the handler with the stream observer.
     * This should be called from the beforeStart callback.
     */
    public void initialize(StreamObserver<T> observer) {
        if (isInitialized.compareAndSet(false, true)) {
            this.requestObserver = observer;
            logger.debug("Request handler initialized for stream: {}", streamId);

            // Flush any queued requests that were added before initialization
            CompletableFuture.runAsync(this::flush);
        }
    }

    /**
     * Queue a request for sending.
     * This method is thread-safe and non-blocking.
     */
    public CompletableFuture<Boolean> sendRequest(T request) {
        if (isCompleted.get()) {
            logger.warn("WARN: Cannot send request on completed stream: {}", streamId);
            return CompletableFuture.completedFuture(false);
        }

        // Add to queue (non-blocking)
        requestQueue.offer(request);
        pendingRequests.incrementAndGet();

        // Try to flush immediately if initialized
        if (isInitialized.get()) {
            return CompletableFuture.supplyAsync(this::flush);
        } else {
            // Stream not ready yet, request will be sent when initialized
            return CompletableFuture.completedFuture(true);
        }
    }

    /**
     * Flush all pending requests.
     * Uses a lock to ensure only one thread sends at a time.
     */
    public boolean flush() {
        if (!isInitialized.get() || !sendLock.tryLock()) {
            // Stream not ready or another thread is already flushing
            return true;
        }

        try {
            T request;
            while ((request = requestQueue.poll()) != null) {
                if (isCompleted.get()) {
                    return false;
                }

                try {
                    if (requestObserver != null) {
                        requestObserver.onNext(request);
                        totalSent++;
                        pendingRequests.decrementAndGet();
                    } else {
                        // Observer became null, re-queue the request
                        requestQueue.offer(request);
                        return false;
                    }
                } catch (Exception e) {
                    logger.error("Error sending request on stream {}: {}", streamId, e.getMessage());
                    totalErrors++;
                    return false;
                }
            }
            return true;
        } finally {
            sendLock.unlock();
        }
    }

    /**
     * Complete the stream and send any remaining requests.
     */
    public CompletableFuture<Void> complete() {
        if (!isCompleted.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            sendLock.lock();
            try {
                // Flush any remaining requests
                flush();

                // Complete the stream
                if (requestObserver != null) {
                    try {
                        requestObserver.onCompleted();
                        logger.info("Stream {} completed. Sent: {}, Errors: {}, Pending: {}",
                                streamId, totalSent, totalErrors, pendingRequests.get());
                    } catch (Exception e) {
                        logger.error("Error completing stream {}: {}", streamId, e.getMessage());
                    }
                }
            } finally {
                sendLock.unlock();
            }
        });
    }

    /**
     * Send an error and complete the stream.
     */
    public void sendError(Throwable error) {
        if (!isCompleted.compareAndSet(false, true)) {
            return;
        }

        sendLock.lock();
        try {
            if (requestObserver != null) {
                requestObserver.onError(error);
            }
        } catch (Exception e) {
            logger.error("Error sending error on stream {}: {}", streamId, e.getMessage());
        } finally {
            sendLock.unlock();
        }
    }

    /**
     * Check if the stream is ready to send requests.
     */
    public boolean isReady() {
        return isInitialized.get() && !isCompleted.get() && requestObserver != null;
    }

    /**
     * Check if the stream is completed.
     */
    public boolean isCompleted() {
        return isCompleted.get();
    }

    /**
     * Get simple metrics.
     */
    public StreamMetrics getMetrics() {
        return new StreamMetrics(totalSent, totalErrors, pendingRequests.get());
    }

    /**
     * Force flush all pending requests (blocking).
     * Use with caution - mainly for cleanup scenarios.
     */
    public void forceFlush() {
        sendLock.lock();
        try {
            flush();
        } finally {
            sendLock.unlock();
        }
    }

    public static class StreamMetrics {
        public final long sent;
        public final long errors;
        public final int pending;

        StreamMetrics(long sent, long errors, int pending) {
            this.sent = sent;
            this.errors = errors;
            this.pending = pending;
        }

        @Override
        public String toString() {
            return String.format("StreamMetrics{sent=%d, errors=%d, pending=%d}", sent, errors, pending);
        }
    }
}