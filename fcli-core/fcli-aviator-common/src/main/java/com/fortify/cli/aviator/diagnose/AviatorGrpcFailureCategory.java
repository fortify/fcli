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
package com.fortify.cli.aviator.diagnose;

public enum AviatorGrpcFailureCategory {
    NONE(null),
    NON_GRPC_HTTP("non-grpc-http-response"),
    TLS("grpc-tls-handshake-failed"),
    NO_RESPONSE("grpc-no-response");

    private final String wireId;

    AviatorGrpcFailureCategory(String wireId) {
        this.wireId = wireId;
    }

    /** Machine-readable id for evidence JSON; null for {@link #NONE}. */
    public String wireId() {
        return wireId;
    }
}
