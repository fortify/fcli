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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.util.Optional;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.netty.channel.ConnectTimeoutException;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultHttpHeaders;
import io.grpc.netty.shaded.io.netty.handler.proxy.HttpProxyHandler.HttpProxyConnectException;
import io.grpc.netty.shaded.io.netty.handler.proxy.ProxyConnectException;

class AviatorConnectivityFailureKindTest {
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

    @ParameterizedTest
    @ValueSource(strings = {"Connection refused", "Connection refused: getsockopt", "Connection refused: no further information"})
    void classifiesConnectExceptionAsDirectRefusalWhenNotProxied(String reason) {
        var cause = new ConnectException(reason);
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
    void leavesCertificateFailuresToPkixGuidance() {
        var pkix = new SSLHandshakeException("PKIX path building failed");
        pkix.initCause(new CertificateException("untrusted"));
        assertEquals(Optional.empty(), classify(Status.UNAVAILABLE, pkix, DIRECT));
        assertEquals(Optional.empty(), classify(Status.UNAVAILABLE, new CertPathValidatorException("untrusted"), DIRECT));
    }

    @Test
    void classifiesOtherTlsFailures() {
        assertEquals(Optional.of(AviatorConnectivityFailureKind.TLS_FAILURE),
            classify(Status.UNAVAILABLE, new SSLException("Unsupported or unrecognized SSL message"), PROXIED));
        assertEquals(Optional.of(AviatorConnectivityFailureKind.TLS_FAILURE),
            classify(Status.UNAVAILABLE, new SSLHandshakeException("handshake timed out"), DIRECT));
    }

    @Test
    void usesNeutralKindForProxyFailuresWithoutFcliProxy() {
        var cause = new HttpProxyConnectException("status: 407", new DefaultHttpHeaders());
        assertEquals(Optional.of(AviatorConnectivityFailureKind.UNAVAILABLE), classify(Status.UNAVAILABLE, cause, DIRECT));
        assertEquals(Optional.of(AviatorConnectivityFailureKind.DEADLINE_EXCEEDED),
            classify(Status.DEADLINE_EXCEEDED, new ProxyConnectException("timeout"), DIRECT));
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
    void doesNotInferRefusalFromAnAmbiguousConnectException() {
        var cause = new ConnectException("Connection refused: openssl-handshake_failure.example/10.0.0.1:443");
        assertEquals(Optional.of(AviatorConnectivityFailureKind.UNAVAILABLE), classify(Status.UNAVAILABLE, cause, DIRECT));
    }

    @Test
    void doesNotMisreportNetworkOrUnknownConnectFailuresAsRefusal() {
        for ( var reason : new String[] {"Network is unreachable", "Connection timed out", "Permission denied", null} ) {
            assertEquals(Optional.of(AviatorConnectivityFailureKind.UNAVAILABLE),
                classify(Status.UNAVAILABLE, new ConnectException(reason), DIRECT));
        }
    }

    @Test
    void recognizesRefusalInsideNettyAnnotatedCause() {
        var cause = new ConnectException("Connection refused: openssl-handshake_failure.example/10.0.0.1:443");
        cause.initCause(new ConnectException("Connection refused"));
        assertEquals(Optional.of(AviatorConnectivityFailureKind.DIRECT_REFUSED), classify(Status.UNAVAILABLE, cause, DIRECT));
    }

    @ParameterizedTest
    @EnumSource(AviatorConnectivityFailureKind.class)
    void everyFailureHasASingleLineActionableMessage(AviatorConnectivityFailureKind kind) {
        for ( var context : new AviatorConnectionContext[] {DIRECT, PROXIED} ) {
            if ( !context.isProxied() && kind.name().startsWith("PROXY_") ) { continue; }
            var message = kind.formatMessage(context);
            assertEquals(1, message.lines().count(), message);
            assertTrue(message.contains("; check ") || message.contains("; try "), message);
            assertFalse(message.endsWith("."), message);
            assertFalse(message.contains("null"), message);
        }
    }

    @Test
    void messagesNameTheFailingEndpoint() {
        assertEquals("Proxy localhost:33444 refused the connection; check the proxy host and port",
            AviatorConnectivityFailureKind.PROXY_REFUSED.formatMessage(PROXIED));
        assertEquals("Aviator server aviator.example.com:443 refused the connection; check the Aviator URL",
            AviatorConnectivityFailureKind.DIRECT_REFUSED.formatMessage(DIRECT));
        assertEquals("Timed out connecting to proxy localhost:33444; check network access and firewall rules",
            AviatorConnectivityFailureKind.CONNECT_TIMEOUT.formatMessage(PROXIED));
        assertEquals("Aviator server aviator.example.com:443 did not respond within 30 seconds; "
                + "check network access and proxy settings, or try again later",
            AviatorConnectivityFailureKind.DEADLINE_EXCEEDED.formatMessage(DIRECT));
    }

    private static Optional<AviatorConnectivityFailureKind> classify(Status status, Throwable cause, AviatorConnectionContext context) {
        var exception = new StatusRuntimeException(cause==null ? status : status.withCause(cause));
        return AviatorConnectivityFailureKind.classify(exception, context);
    }
}
