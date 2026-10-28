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

import java.util.concurrent.ThreadLocalRandom;

import com.fortify.cli.aviator.util.Constants;

/** Backoff used when the server rejects a request as overloaded (SERVER_BUSY). */
final class ServerBusyBackoff {
    static final String STATUS = "SERVER_BUSY";

    private ServerBusyBackoff() {}

    static boolean isServerBusy(String status) {
        return STATUS.equalsIgnoreCase(status);
    }

    /** Exponential delay for the given 1-based attempt, capped at {@code MAX_DELAY_MS}, plus jitter. */
    static long delayMs(int attempt) {
        long exponential = Constants.BASE_DELAY_MS * (1L << Math.min(attempt - 1, 20));
        return Math.min(Constants.MAX_DELAY_MS, exponential) + ThreadLocalRandom.current().nextLong(100);
    }
}
