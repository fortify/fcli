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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

class RequestHandlerTest {
    @Test
    @Timeout(10)
    void contendedSendDoesNotReportSuccessBeforeTheQueueIsFlushed() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var events = new ArrayList<String>();
        var handler = new RequestHandler<String>("stream");
        handler.sendRequest("one");
        handler.initialize(new StreamObserver<>() {
            public void onNext(String value) {
                if ("one".equals(value)) {
                    entered.countDown();
                    try {
                        assertTrue(release.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                }
                events.add(value);
            }
            public void onError(Throwable error) { }
            public void onCompleted() { }
        });
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        var send = handler.sendRequest("two");
        try {
            assertThrows(TimeoutException.class, () -> send.get(100, TimeUnit.MILLISECONDS));
        } finally {
            release.countDown();
        }
        assertTrue(send.get(5, TimeUnit.SECONDS));
        handler.complete().join();
        assertEquals(List.of("one", "two"), events);
    }

    @Test
    @Timeout(10)
    void initializationFlushReportsAQueuedSendFailure() throws Exception {
        var handler = new RequestHandler<String>("stream");
        var failure = Status.UNAUTHENTICATED.asRuntimeException();
        handler.sendRequest("queued");
        handler.initialize(new StreamObserver<>() {
            public void onNext(String value) { throw failure; }
            public void onError(Throwable error) {}
            public void onCompleted() {}
        });
        assertEquals(failure, handler.getSendFailure().get(5, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(10)
    void abortDiscardsQueuedRequestsWithoutWaitingForABlockedSendOrHalfClosing() throws Exception {
        var events = new CopyOnWriteArrayList<String>();
        var sending = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var cancelled = new CountDownLatch(1);
        var handler = new RequestHandler<String>("failed-attempt");
        handler.initialize(new StreamObserver<>() {
            public void onNext(String value) {
                events.add(value);
                sending.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Send was not released");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
            public void onError(Throwable error) { events.add("error"); cancelled.countDown(); }
            public void onCompleted() { events.add("completed"); }
        });
        handler.sendRequest("one");
        assertTrue(sending.await(5, TimeUnit.SECONDS));
        handler.sendRequest("two");
        try {
            handler.abort(new Exception("Injected disconnect"));
            assertTrue(handler.isCompleted());
            assertTrue(handler.getRequestQueue().isEmpty());
        } finally {
            release.countDown();
        }
        assertTrue(cancelled.await(5, TimeUnit.SECONDS));
        assertEquals(List.of("one", "error"), events);
    }

    @Test
    void completeFlushesQueuedRequestsBeforeHalfClose() {
        var events = new ArrayList<String>();
        var handler = new RequestHandler<String>("stream");
        handler.sendRequest("one");
        handler.sendRequest("two");
        handler.initialize(observer(events));

        handler.complete().join();

        assertEquals(List.of("one", "two", "completed"), events);
        assertTrue(handler.isCompleted());
    }

    private StreamObserver<String> observer(List<String> events) {
        return new StreamObserver<>() {
            @Override public void onNext(String value) { events.add(value); }
            @Override public void onError(Throwable throwable) { events.add("error"); }
            @Override public void onCompleted() { events.add("completed"); }
        };
    }
}