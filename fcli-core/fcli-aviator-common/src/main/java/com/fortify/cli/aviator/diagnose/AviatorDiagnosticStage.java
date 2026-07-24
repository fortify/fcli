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
 * Diagnostic stage identifiers for report rows and evidence.
 * <p>
 * Transport pipeline ({@code AviatorConnectionDiagnostics}) runs
 * {@link #ENDPOINT} then DNS → TCP → optional {@link #PROXY} → {@link #TLS} → {@link #GRPC}.
 * {@link #TOKEN} and {@link #ADMIN} are optional product-layer credential stages appended by
 * the connection diagnose helper only when credentials were requested; they are not part of
 * the transport skip chain.
 */
public enum AviatorDiagnosticStage {
    ENDPOINT("endpoint", "Aviator endpoint configuration validation"),
    DNS("dns", "DNS resolution"),
    TCP("tcp", "TCP connectivity"),
    PROXY("proxy", "HTTP proxy CONNECT"),
    TLS("tls", "TLS handshake"),
    GRPC("grpc", "gRPC request/response reachability"),
    /** Optional user-token check (session or {@code --url --token}); not a transport stage. */
    TOKEN("token", "Aviator token validation"),
    /** Optional admin-config credential check; not a transport stage. */
    ADMIN("admin", "Aviator admin credential validation");

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