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
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.util.Optional;

import javax.net.ssl.SSLException;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fortify.cli.aviator.util.Constants;

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
    TLS_FAILURE,
    UNAVAILABLE,
    DEADLINE_EXCEEDED;

    /**
     * Leaves certificate failures to the fcli PKIX trust-store guidance, and application
     * statuses to the existing exception mapping.
     */
    static Optional<AviatorConnectivityFailureKind> classify(StatusRuntimeException e, AviatorConnectionContext context) {
        var code = e.getStatus().getCode();
        if ( code!=Status.Code.UNAVAILABLE && code!=Status.Code.DEADLINE_EXCEEDED ) {
            return Optional.empty();
        }
        if ( isCertificateFailure(e) ) {
            return Optional.empty();
        }
        if ( has(e, SSLException.class) ) {
            return Optional.of(TLS_FAILURE);
        }
        return Optional.of(classifyTransport(e, context, code));
    }

    // JDK PKIX and host name failures always carry one of these causes; other
    // handshake failures, such as timeouts or protocol mismatches, are TLS_FAILURE.
    private static boolean isCertificateFailure(Throwable e) {
        return has(e, CertificateException.class) || has(e, CertPathValidatorException.class);
    }

    // The order is significant: all Netty connect and proxy exceptions extend ConnectException.
    private static AviatorConnectivityFailureKind classifyTransport(Throwable e, AviatorConnectionContext context, Status.Code code) {
        var neutral = code==Status.Code.DEADLINE_EXCEEDED ? DEADLINE_EXCEEDED : UNAVAILABLE;
        if ( has(e, ProxyConnectException.class) ) {
            // A JVM or GRPC_PROXY_EXP proxy can be active without an fcli proxy; fcli cannot name it.
            if ( !context.isProxied() ) { return neutral; }
            return has(e, HttpProxyConnectException.class) ? PROXY_REJECTED : PROXY_NEGOTIATION_FAILED;
        }
        if ( has(e, ConnectTimeoutException.class) ) { return CONNECT_TIMEOUT; }
        // With an HTTP CONNECT proxy, the proxy is the only TCP peer of the client;
        // a target refusal arrives as HttpProxyConnectException instead.
        if ( isConnectionRefused(e) ) { return context.isProxied() ? PROXY_REFUSED : DIRECT_REFUSED; }
        if ( has(e, UnknownHostException.class) || has(e, UnresolvedAddressException.class) ) { return DNS_FAILURE; }
        return neutral;
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
        var server = "Aviator server " + context.target().display();
        var proxy = context.isProxied() ? "proxy " + context.proxy().display() : null;
        return switch (this) {
            case PROXY_REJECTED -> "Proxy " + context.proxy().display() + " rejected the connection to " + server
                + "; check the proxy credentials and access rules";
            case PROXY_NEGOTIATION_FAILED -> "Could not connect to " + server + " through " + proxy
                + "; check that the proxy allows HTTPS connections to the Aviator server";
            case CONNECT_TIMEOUT -> "Timed out connecting to " + (proxy==null ? server : proxy)
                + "; check network access and firewall rules";
            case PROXY_REFUSED -> "Proxy " + context.proxy().display() + " refused the connection; check the proxy host and port";
            case DIRECT_REFUSED -> server + " refused the connection; check the Aviator URL";
            case DNS_FAILURE -> proxy==null
                ? "Could not resolve the host name of " + server + "; check the Aviator URL"
                : "Could not resolve the host name of " + proxy + " or " + server + "; check the proxy settings and the Aviator URL";
            case TLS_FAILURE -> "Could not set up a TLS connection to " + server
                + "; check that the Aviator URL uses the HTTPS port of the Aviator server";
            case UNAVAILABLE -> "Cannot reach " + server + "; check the Aviator URL, network access, and proxy settings";
            case DEADLINE_EXCEEDED -> server + " did not respond within " + Constants.DEFAULT_TIMEOUT_SECONDS
                + " seconds; check network access and proxy settings, or try again later";
        };
    }
}
