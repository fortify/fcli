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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class AviatorOperationIdTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void generatesLowercaseUuidV7WithEmbeddedCreationTime() {
        String id = AviatorOperationId.generate(clock);

        assertTrue(AviatorOperationId.isValid(id), id);
        assertEquals('7', id.charAt(14));
        assertEquals(clock.instant(), AviatorOperationId.issuedAt(id));
    }

    @Test
    void generatedIdsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            ids.add(AviatorOperationId.generate(clock));
        }
        assertEquals(5000, ids.size());
    }

    @Test
    void rejectsIdsTheServerWouldRefuse() {
        for (String invalid : new String[] {null, "", "L1-abc", "not-a-uuid", "123e4567-e89b-42d3-a456-426614174000",
                AviatorOperationId.generate(clock).toUpperCase()}) {
            assertFalse(AviatorOperationId.isValid(invalid), String.valueOf(invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> AviatorOperationId.issuedAt("nope"));
    }

    @Test
    void expiryFollowsTheEmbeddedTimestamp() {
        String id = AviatorOperationId.generate(clock);
        Clock later = Clock.offset(clock, Duration.ofHours(23));
        Clock muchLater = Clock.offset(clock, Duration.ofHours(25));

        assertFalse(AviatorOperationId.isExpired(id, AviatorOperationId.DEFAULT_MAX_AGE, later));
        assertTrue(AviatorOperationId.isExpired(id, AviatorOperationId.DEFAULT_MAX_AGE, muchLater));
        assertNotEquals(id, AviatorOperationId.generate(clock));
    }
}
