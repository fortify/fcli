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

/**
 * Transport diagnostic stage identifiers for report rows and evidence.
 * <p>
 * Pipeline: {@link #ENDPOINT} then DNS → TCP → optional {@link #PROXY} → {@link #TLS} → {@link #GRPC}.
 * Product-layer credential stages (token/admin) are not part of this enum; they append
 * string stage ids from the connection diagnose helper only.
 */
public enum AviatorDiagnosticStage {
    ENDPOINT("endpoint", "Aviator endpoint configuration validation"),
    DNS("dns", "DNS resolution"),
    TCP("tcp", "TCP connectivity"),
    PROXY("proxy", "HTTP proxy CONNECT"),
    TLS("tls", "TLS handshake"),
    GRPC("grpc", "gRPC request/response reachability");

    private final String id;
    private final String description;

    AviatorDiagnosticStage(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }
}
