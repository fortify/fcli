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

import static com.fortify.cli.aviator.grpc.AviatorConnectivityFailureKind.CONNECT_TIMEOUT;
import static com.fortify.cli.aviator.grpc.AviatorConnectivityFailureKind.DEADLINE_EXCEEDED;
import static com.fortify.cli.aviator.grpc.AviatorConnectivityFailureKind.DIRECT_REFUSED;
import static com.fortify.cli.aviator.grpc.AviatorConnectivityFailureKind.DNS_FAILURE;
import static com.fortify.cli.aviator.grpc.AviatorConnectivityFailureKind.PROXY_NEGOTIATION_FAILED;
import static com.fortify.cli.aviator.grpc.AviatorConnectivityFailureKind.PROXY_REFUSED;
import static com.fortify.cli.aviator.grpc.AviatorConnectivityFailureKind.PROXY_REJECTED;
import static com.fortify.cli.aviator.grpc.AviatorConnectivityFailureKind.UNAVAILABLE;

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

/**
 * Maps a failed unary gRPC call to an {@link AviatorConnectivityFailureKind}, based only on
 * the status code and the exception types in the cause chain. It never reads status
 * descriptions or exception messages.
 */
final class AviatorConnectivityFailureClassifier {
    private AviatorConnectivityFailureClassifier() {}

    /**
     * Returns the failure kind, or an empty Optional if the failure is not a connectivity
     * failure (other status codes, or TLS failures, which keep their current handling).
     */
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
        if ( has(e, ConnectException.class) ) { return context.isProxied() ? PROXY_REFUSED : DIRECT_REFUSED; }
        if ( has(e, UnknownHostException.class) || has(e, UnresolvedAddressException.class) ) { return DNS_FAILURE; }
        return code==Status.Code.DEADLINE_EXCEEDED ? DEADLINE_EXCEEDED : UNAVAILABLE;
    }

    private static boolean has(Throwable e, Class<? extends Throwable> type) {
        return ExceptionUtils.indexOfType(e, type) >= 0;
    }
}
