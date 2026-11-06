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

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Operation identifier shared by all Aviator clients: a lowercase UUIDv7 that is generated once per logical
 * operation and reused on every retry. The creation time is embedded in the first 48 bits, which is how the
 * server decides that an identifier is too old to be deduplicated.
 */
public final class AviatorOperationId {
    /** Mirrors the server default; the effective value is advertised through {@code GetServerCapabilities}. */
    public static final Duration DEFAULT_MAX_AGE = Duration.ofHours(24);
    private static final Pattern UUID_V7 = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private AviatorOperationId() {}

    public static String generate() {
        return generate(Clock.systemUTC());
    }

    static String generate(Clock clock) {
        long millis = clock.millis();
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);
        long msb = (millis & 0xFFFFFFFFFFFFL) << 16 | 0x7000L | ((random[0] & 0x0F) << 8) | (random[1] & 0xFF);
        long lsb = 0x8000000000000000L | ((long) (random[2] & 0x3F) << 56);
        for (int i = 3; i < 10; i++) {
            lsb |= (long) (random[i] & 0xFF) << (8 * (9 - i));
        }
        return new UUID(msb, lsb).toString();
    }

    public static boolean isValid(String operationId) {
        return operationId != null && UUID_V7.matcher(operationId).matches();
    }

    public static Instant issuedAt(String operationId) {
        if (!isValid(operationId)) {
            throw new IllegalArgumentException("Not a UUIDv7 operation id");
        }
        return Instant.ofEpochMilli(Long.parseUnsignedLong(operationId.substring(0, 8) + operationId.substring(9, 13), 16));
    }

    /** True when the identifier is older than the server will deduplicate, so a retry would be refused. */
    public static boolean isExpired(String operationId, Duration maxAge, Clock clock) {
        return issuedAt(operationId).isBefore(clock.instant().minus(maxAge));
    }
}
