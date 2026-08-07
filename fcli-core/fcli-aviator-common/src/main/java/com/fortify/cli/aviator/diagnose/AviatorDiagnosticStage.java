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
    ENDPOINT("endpoint", "Aviator endpoint configuration validation", "Endpoint"),
    DNS("dns", "DNS resolution", "DNS"),
    TCP("tcp", "TCP connectivity", "TCP"),
    PROXY("proxy", "HTTP proxy CONNECT", "Proxy"),
    TLS("tls", "TLS handshake", "TLS"),
    GRPC("grpc", "gRPC request/response reachability", "gRPC");

    private final String id;
    private final String description;
    /** Human-readable label for log lines (e.g. {@code Endpoint}, {@code gRPC}). */
    private final String displayName;

    AviatorDiagnosticStage(String id, String description, String displayName) {
        this.id = id;
        this.description = description;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }

    public String displayName() {
        return displayName;
    }

    /** Resolve wire id to display name; falls back to a simple capitalization of {@code stageId}. */
    public static String displayNameFor(String stageId) {
        if (stageId == null || stageId.isBlank()) {
            return "Stage";
        }
        for (var stage : values()) {
            if (stage.id.equals(stageId)) {
                return stage.displayName;
            }
        }
        return Character.toUpperCase(stageId.charAt(0)) + stageId.substring(1);
    }
}
