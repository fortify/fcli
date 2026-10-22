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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.security.cert.CertificateException;
import java.util.Optional;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.netty.channel.ConnectTimeoutException;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultHttpHeaders;
import io.grpc.netty.shaded.io.netty.handler.proxy.HttpProxyHandler.HttpProxyConnectException;
import io.grpc.netty.shaded.io.netty.handler.proxy.ProxyConnectException;

class AviatorConnectivityFailureClassifierTest {
    private static final AviatorConnectionContext DIRECT =
            new AviatorConnectionContext(new AviatorEndpoint("aviator.example.com", 443), null);
    private static final AviatorConnectionContext PROXIED =
            new AviatorConnectionContext(new AviatorEndpoint("aviator.example.com", 443), new AviatorEndpoint("localhost", 33444));

    @Test
    void classifiesProxyRejectionBeforeOtherConnectExceptions() {
        var cause = new HttpProxyConnectException("status: 407", new DefaultHttpHeaders());
        assertEquals(Optional.of(AviatorConnectivityFailureKind.PROXY_REJECTED), classify(Status.UNAVAILABLE, cause, PROXIED));
    }

    @Test
    void classifiesOtherProxyFailuresAsNegotiationFailure() {
        var cause = new ProxyConnectException("timeout");
        assertEquals(Optional.of(AviatorConnectivityFailureKind.PROXY_NEGOTIATION_FAILED), classify(Status.UNAVAILABLE, cause, PROXIED));
    }

    @Test
    void classifiesConnectTimeoutBeforeRefusal() {
        var cause = new ConnectTimeoutException("connection timed out");
        assertEquals(Optional.of(AviatorConnectivityFailureKind.CONNECT_TIMEOUT), classify(Status.UNAVAILABLE, cause, DIRECT));
    }

    @Test
    void classifiesConnectExceptionAsProxyRefusalWhenProxied() {
        var cause = new ConnectException("Connection refused");
        assertEquals(Optional.of(AviatorConnectivityFailureKind.PROXY_REFUSED), classify(Status.UNAVAILABLE, cause, PROXIED));
    }

    @Test
    void classifiesConnectExceptionAsDirectRefusalWhenNotProxied() {
        var cause = new ConnectException("Connection refused");
        assertEquals(Optional.of(AviatorConnectivityFailureKind.DIRECT_REFUSED), classify(Status.UNAVAILABLE, cause, DIRECT));
    }

    @Test
    void classifiesNameResolutionFailures() {
        assertEquals(Optional.of(AviatorConnectivityFailureKind.DNS_FAILURE),
            classify(Status.UNAVAILABLE, new UnknownHostException("host"), DIRECT));
        assertEquals(Optional.of(AviatorConnectivityFailureKind.DNS_FAILURE),
            classify(Status.UNAVAILABLE, new UnresolvedAddressException(), PROXIED));
    }

    @Test
    void usesNeutralKindsWithoutTransportEvidence() {
        assertEquals(Optional.of(AviatorConnectivityFailureKind.UNAVAILABLE), classify(Status.UNAVAILABLE, null, DIRECT));
        assertEquals(Optional.of(AviatorConnectivityFailureKind.DEADLINE_EXCEEDED), classify(Status.DEADLINE_EXCEEDED, null, DIRECT));
    }

    @Test
    void ignoresTlsFailures() {
        assertEquals(Optional.empty(), classify(Status.UNAVAILABLE, new SSLHandshakeException("handshake"), DIRECT));
    }

    @ParameterizedTest
    @EnumSource(value = Status.Code.class, names = {"UNAVAILABLE", "DEADLINE_EXCEEDED"}, mode = EnumSource.Mode.EXCLUDE)
    void ignoresOtherStatusCodes(Status.Code code) {
        assertEquals(Optional.empty(), classify(Status.fromCode(code), new ConnectException("Connection refused"), DIRECT));
    }

    @Test
    void terminatesOnCyclicCauseChain() {
        var first = new RuntimeException("first");
        var second = new RuntimeException("second", first);
        first.initCause(second);

        assertEquals(Optional.of(AviatorConnectivityFailureKind.UNAVAILABLE), classify(Status.UNAVAILABLE, first, DIRECT));
    }

    @Test
    void ignoresNestedCertificateFailures() {
        var cause = new RuntimeException(new CertificateException("untrusted"));
        assertEquals(Optional.empty(), classify(Status.UNAVAILABLE, cause, DIRECT));
    }

    @Test
    void doesNotReadExceptionMessages() {
        var cause = new ConnectException("Connection refused: openssl-handshake_failure.example/10.0.0.1:443");
        assertEquals(Optional.of(AviatorConnectivityFailureKind.DIRECT_REFUSED), classify(Status.UNAVAILABLE, cause, DIRECT));
    }

    private static Optional<AviatorConnectivityFailureKind> classify(Status status, Throwable cause, AviatorConnectionContext context) {
        var exception = new StatusRuntimeException(cause==null ? status : status.withCause(cause));
        return AviatorConnectivityFailureClassifier.classify(exception, context);
    }
}
