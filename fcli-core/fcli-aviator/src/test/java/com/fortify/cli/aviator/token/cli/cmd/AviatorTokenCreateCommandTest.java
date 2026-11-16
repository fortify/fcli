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
package com.fortify.cli.aviator.token.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AviatorTokenCreateCommandTest {
    /** UUIDv7 whose embedded timestamp is the given instant. */
    private static String operationIdAt(Instant instant) {
        long millis = instant.toEpochMilli();
        long msb = (millis & 0xFFFFFFFFFFFFL) << 16 | 0x7000L;
        long lsb = 0x8000000000000000L | 1L;
        return new UUID(msb, lsb).toString();
    }

    @Test
    void startDateFollowsTheOperationIdSoARerunAfterUtcMidnightSignsTheSamePayload() {
        String id = operationIdAt(Instant.parse("2026-11-03T23:59:58Z"));

        assertEquals("2026-11-03", AviatorTokenCreateCommand.startDate(id));
    }
}
