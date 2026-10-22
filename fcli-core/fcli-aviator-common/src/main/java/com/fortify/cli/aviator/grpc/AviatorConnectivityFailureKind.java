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

enum AviatorConnectivityFailureKind {
    PROXY_REJECTED,
    PROXY_NEGOTIATION_FAILED,
    CONNECT_TIMEOUT,
    PROXY_REFUSED,
    DIRECT_REFUSED,
    DNS_FAILURE,
    UNAVAILABLE,
    DEADLINE_EXCEEDED;

    String formatMessage(AviatorConnectionContext context) {
        return switch (this) {
            case PROXY_REJECTED -> "Proxy " + context.proxy().display() + " rejected the connection to the Aviator server\n"
                + "Check the proxy credentials, access rules, and whether the Aviator service is available";
            case PROXY_NEGOTIATION_FAILED -> "Could not open a tunnel to the Aviator server through proxy "
                + context.proxy().display() + "\nCheck that the proxy is reachable and allows connections to the Aviator server";
            case CONNECT_TIMEOUT -> "Connection to " + context.firstHop().display()
                + " timed out\nCheck network access, firewall rules, and proxy settings";
            case PROXY_REFUSED -> "Connection to proxy " + context.proxy().display()
                + " was refused\nCheck that the proxy is running and that its host and port are correct";
            case DIRECT_REFUSED -> "Connection to Aviator server " + context.target().display()
                + " was refused\nCheck the Aviator URL and that the Aviator service is available";
            case DNS_FAILURE -> "Could not resolve a host name for the Aviator connection\n"
                + "Check the Aviator URL, the proxy host, and the DNS settings";
            case UNAVAILABLE -> "The Aviator server is unavailable or cannot be reached\n"
                + "Check the Aviator URL, network access, and proxy settings, then try again";
            case DEADLINE_EXCEEDED -> "The Aviator request timed out\n"
                + "Check network access and proxy settings; if they are correct, try again later";
        };
    }
}
