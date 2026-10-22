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

import java.util.Optional;

/**
 * Kinds of Aviator connectivity failure, each with one user-facing message.
 * To add a kind, add a constant here and one rule in {@link AviatorConnectivityFailureClassifier}.
 */
enum AviatorConnectivityFailureKind {
    PROXY_REJECTED(EndpointRole.PROXY,
        "Proxy %s rejected the connection to the Aviator server",
        "Check the proxy credentials and the proxy access rules"),
    PROXY_NEGOTIATION_FAILED(EndpointRole.PROXY,
        "Could not open a tunnel to the Aviator server through proxy %s",
        "Check that the proxy is reachable and allows connections to the Aviator server"),
    CONNECT_TIMEOUT(EndpointRole.FIRST_HOP,
        "Connection to %s timed out",
        "Check network access, firewall rules, and proxy settings"),
    PROXY_REFUSED(EndpointRole.PROXY,
        "Connection to proxy %s was refused",
        "Check that the proxy is running and that its host and port are correct"),
    DIRECT_REFUSED(EndpointRole.TARGET,
        "Connection to Aviator server %s was refused",
        "Check the Aviator URL and that the Aviator service is available"),
    DNS_FAILURE(EndpointRole.NONE,
        "Could not resolve a host name for the Aviator connection",
        "Check the Aviator URL, the proxy host, and the DNS settings"),
    UNAVAILABLE(EndpointRole.NONE,
        "The Aviator server is unavailable or cannot be reached",
        "Check the Aviator URL, network access, and proxy settings, then try again"),
    DEADLINE_EXCEEDED(EndpointRole.NONE,
        "The Aviator request timed out",
        "Check network access and proxy settings; if they are correct, try again later");

    private final EndpointRole endpointRole;
    private final String summaryTemplate;
    private final String guidance;

    AviatorConnectivityFailureKind(EndpointRole endpointRole, String summaryTemplate, String guidance) {
        this.endpointRole = endpointRole;
        this.summaryTemplate = summaryTemplate;
        this.guidance = guidance;
    }

    String formatMessage(AviatorConnectionContext context) {
        var summary = endpointRole.resolve(context)
            .map(endpoint -> String.format(summaryTemplate, endpoint.display()))
            .orElse(summaryTemplate);
        return summary + "\n" + guidance;
    }

    private enum EndpointRole {
        TARGET, PROXY, FIRST_HOP, NONE;

        Optional<AviatorEndpoint> resolve(AviatorConnectionContext context) {
            return Optional.ofNullable(switch (this) {
                case TARGET -> context.target();
                case PROXY -> context.proxy();
                case FIRST_HOP -> context.firstHop();
                case NONE -> null;
            });
        }
    }
}
