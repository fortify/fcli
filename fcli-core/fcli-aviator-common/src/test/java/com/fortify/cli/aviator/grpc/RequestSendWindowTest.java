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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.util.Constants;

class RequestSendWindowTest {
    private static final Function<String, String> ID = Function.identity();

    @Test
    void neverSendsMoreThanWindowSizeUntilSlotsAreReleased() {
        var window = new RequestSendWindow<>(10, ID);
        window.enqueue(requests(600));

        assertEquals(10, window.drainSendable().size());
        assertEquals(0, window.drainSendable().size());
        assertEquals(10, window.inFlightCount());
        assertEquals(590, window.queuedCount());
    }

    @Test
    void releasingOneSlotAllowsExactlyOneMoreRequest() {
        var window = new RequestSendWindow<>(10, ID);
        window.enqueue(requests(30));
        List<String> first = window.drainSendable();

        assertTrue(window.release(first.get(0)));
        List<String> next = window.drainSendable();

        assertEquals(List.of("req-10"), next);
        assertEquals(10, window.inFlightCount());
    }

    @Test
    void inFlightStaysBoundedWhileDrainingLargeBacklog() {
        var window = new RequestSendWindow<>(10, ID);
        window.enqueue(requests(600));
        List<String> sent = new ArrayList<>(window.drainSendable());
        int completed = 0;
        int maxInFlight = window.inFlightCount();

        while (completed < 600) {
            assertTrue(window.release(sent.get(completed)));
            completed++;
            sent.addAll(window.drainSendable());
            maxInFlight = Math.max(maxInFlight, window.inFlightCount());
        }

        assertEquals(10, maxInFlight);
        assertEquals(600, sent.size());
        assertEquals(0, window.inFlightCount());
        assertEquals(0, window.queuedCount());
    }

    @Test
    void releaseIsIdempotentAndIgnoresUnknownRequests() {
        var window = new RequestSendWindow<>(2, ID);
        window.enqueue(requests(2));
        window.drainSendable();

        assertTrue(window.release("req-0"));
        assertFalse(window.release("req-0"));
        assertFalse(window.release("unknown"));
        assertEquals(1, window.inFlightCount());
    }

    @Test
    void clearDropsQueuedAndInFlightRequests() {
        var window = new RequestSendWindow<>(2, ID);
        window.enqueue(requests(5));
        window.drainSendable();

        window.clear();

        assertEquals(0, window.inFlightCount());
        assertEquals(0, window.queuedCount());
    }

    @Test
    void rejectsNonPositiveWindow() {
        assertThrows(IllegalArgumentException.class, () -> new RequestSendWindow<>(0, ID));
    }

    @Test
    void serverBusyBackoffGrowsAndIsCapped() {
        assertTrue(ServerBusyBackoff.isServerBusy("server_busy"));
        assertFalse(ServerBusyBackoff.isServerBusy("INTERNAL_ERROR"));

        long first = ServerBusyBackoff.delayMs(1);
        long fourth = ServerBusyBackoff.delayMs(4);
        long huge = ServerBusyBackoff.delayMs(Constants.MAX_RETRIES + 50);

        assertTrue(first >= Constants.BASE_DELAY_MS && first < Constants.BASE_DELAY_MS + 100);
        assertTrue(fourth >= Constants.BASE_DELAY_MS * 8);
        assertTrue(huge >= Constants.MAX_DELAY_MS && huge < Constants.MAX_DELAY_MS + 100);
    }

    private static List<String> requests(int count) {
        List<String> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            requests.add("req-" + i);
        }
        return requests;
    }
}
