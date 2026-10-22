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

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.security.cert.CertificateException;
import java.util.Optional;

import javax.net.ssl.SSLException;

import org.apache.commons.lang3.exception.ExceptionUtils;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.netty.channel.ConnectTimeoutException;
import io.grpc.netty.shaded.io.netty.handler.proxy.HttpProxyHandler.HttpProxyConnectException;
import io.grpc.netty.shaded.io.netty.handler.proxy.ProxyConnectException;

/** Shared classification and messages for ordinary unary request failures. */
enum AviatorConnectivityFailureKind {
    PROXY_REJECTED,
    PROXY_NEGOTIATION_FAILED,
    CONNECT_TIMEOUT,
    PROXY_REFUSED,
    DIRECT_REFUSED,
    DNS_FAILURE,
    UNAVAILABLE,
    DEADLINE_EXCEEDED;

    /** Leaves TLS failures and application statuses to the existing exception handler. */
    static Optional<AviatorConnectivityFailureKind> classify(StatusRuntimeException e, AviatorConnectionContext context) {
        var code = e.getStatus().getCode();
        if ( code!=Status.Code.UNAVAILABLE && code!=Status.Code.DEADLINE_EXCEEDED ) {
            return Optional.empty();
        }
        if ( has(e, SSLException.class) || has(e, CertificateException.class) ) {
            return Optional.empty();
        }
        return Optional.of(classifyTransport(e, context, code));
    }

    // The order is significant: all Netty connect and proxy exceptions extend ConnectException.
    private static AviatorConnectivityFailureKind classifyTransport(Throwable e, AviatorConnectionContext context, Status.Code code) {
        if ( has(e, HttpProxyConnectException.class) ) { return PROXY_REJECTED; }
        if ( has(e, ProxyConnectException.class) ) { return PROXY_NEGOTIATION_FAILED; }
        if ( has(e, ConnectTimeoutException.class) ) { return CONNECT_TIMEOUT; }
        // With an HTTP CONNECT proxy, the proxy is the only TCP peer of the client;
        // a target refusal arrives as HttpProxyConnectException instead.
        if ( isConnectionRefused(e) ) { return context.isProxied() ? PROXY_REFUSED : DIRECT_REFUSED; }
        if ( has(e, UnknownHostException.class) || has(e, UnresolvedAddressException.class) ) { return DNS_FAILURE; }
        return code==Status.Code.DEADLINE_EXCEEDED ? DEADLINE_EXCEEDED : UNAVAILABLE;
    }

    // Java has no refusal-specific exception type. Match complete known reasons, never
    // substrings that could come from a host name; unknown/localized reasons stay neutral.
    private static boolean isConnectionRefused(Throwable e) {
        return ExceptionUtils.getThrowableList(e).stream()
            .filter(ConnectException.class::isInstance)
            .map(Throwable::getMessage)
            .anyMatch(message -> "Connection refused".equalsIgnoreCase(message)
                || "Connection refused: getsockopt".equalsIgnoreCase(message)
                || "Connection refused: no further information".equalsIgnoreCase(message)
                || "connect(..) failed: Connection refused".equalsIgnoreCase(message)
                || "finishConnect(..) failed: Connection refused".equalsIgnoreCase(message));
    }

    private static boolean has(Throwable e, Class<? extends Throwable> type) {
        return ExceptionUtils.indexOfType(e, type) >= 0;
    }

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
