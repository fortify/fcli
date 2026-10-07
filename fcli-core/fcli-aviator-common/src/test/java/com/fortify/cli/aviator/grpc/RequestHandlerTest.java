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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.grpc.stub.StreamObserver;

class RequestHandlerTest {
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