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

/** Replaces control characters in host names before display. */
record AviatorEndpoint(String host, int port) {
    private static final String UNKNOWN_HOST = "unknown";

    AviatorEndpoint {
        host = sanitize(host);
    }

    String display() {
        var isUnbracketedIpv6 = host.contains(":") && !host.startsWith("[");
        return isUnbracketedIpv6 ? "[" + host + "]:" + port : host + ":" + port;
    }

    private static String sanitize(String host) {
        if ( host==null || host.isBlank() ) {
            return UNKNOWN_HOST;
        }
        var result = new StringBuilder(host.length());
        host.codePoints().forEach(c -> result.appendCodePoint(Character.isISOControl(c) ? '?' : c));
        return result.toString();
    }
}
