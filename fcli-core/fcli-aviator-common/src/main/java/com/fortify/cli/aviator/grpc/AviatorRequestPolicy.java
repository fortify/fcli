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

/**
 * Selects how unary Aviator gRPC requests behave when the connection fails.
 * Code that uses this enum reads the flags; it does not compare with specific constants.
 */
public enum AviatorRequestPolicy {
    /** Fail fast on connection failures and report them with the shared connectivity messages. */
    ORDINARY(false, true),
    /** Current behavior: wait for the channel to be ready and use the existing status mapping. */
    UNCHANGED(true, false);

    private final boolean waitForReady;
    private final boolean classifyConnectivityFailures;

    AviatorRequestPolicy(boolean waitForReady, boolean classifyConnectivityFailures) {
        this.waitForReady = waitForReady;
        this.classifyConnectivityFailures = classifyConnectivityFailures;
    }

    public boolean isWaitForReady() {
        return waitForReady;
    }

    public boolean isClassifyConnectivityFailures() {
        return classifyConnectivityFailures;
    }
}
