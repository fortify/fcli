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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;

class AviatorConnectionDiagnosticsTest {
    @Test
    void shouldSkipDependentStagesWhenEndpointIsInvalid() {
        var probe = new FakeDiagnosticProbe();
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(" ", 5, "url");

        assertEquals(5, results.size());
        assertStage(results.get(0), AviatorDiagnosticStage.ENDPOINT, AviatorDiagnosticStatus.FAIL);
        assertStage(results.get(1), AviatorDiagnosticStage.DNS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(2), AviatorDiagnosticStage.TCP, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.WARN);
        assertTrue(diagnostics.hasRequiredFailure(results));
        assertFalse(probe.resolveCalled);
    }

    @Test
    void shouldSkipTransportStagesWhenDnsFails() {
        var probe = new FakeDiagnosticProbe();
        probe.resolveException = new UnknownHostException("host not found");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose("aviator.example.com", 5, "url");

        assertEquals(5, results.size());
        assertStage(results.get(0), AviatorDiagnosticStage.ENDPOINT, AviatorDiagnosticStatus.PASS);
        assertStage(results.get(1), AviatorDiagnosticStage.DNS, AviatorDiagnosticStatus.FAIL);
        assertStage(results.get(2), AviatorDiagnosticStage.TCP, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.WARN);
        assertTrue(diagnostics.hasRequiredFailure(results));
        assertFalse(probe.connectCalled);
        assertFalse(probe.handshakeCalled);
        assertFalse(probe.grpcCalled);
    }

    @Test
    void shouldReportTlsEstablishedGrpcNoResponsePattern() {
        var probe = new FakeDiagnosticProbe();
        probe.grpcResult = new AviatorGrpcReachabilityResult(false, "DEADLINE_EXCEEDED", "deadline exceeded");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose("aviator.example.com", 5, "url");

        assertEquals(5, results.size());
        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.PASS);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.FAIL);
        assertEquals("tls-established-grpc-no-response", results.get(4).evidence().path("pattern").asText());
        assertFalse(diagnostics.hasGrpcResponse(results));
        assertTrue(diagnostics.hasRequiredFailure(results));
    }

    @Test
    void shouldContinueAfterTlsAlpnWarningWhenGrpcResponds() {
        var probe = new FakeDiagnosticProbe();
        probe.tlsResult = new AviatorTlsHandshakeResult("TLSv1.3", "TLS_AES_128_GCM_SHA256", "CN=aviator.example.com", "", "not-used");
        probe.grpcResult = new AviatorGrpcReachabilityResult(true, "UNAUTHENTICATED", "token required");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose("aviator.example.com", 5, "url");

        assertEquals(5, results.size());
        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.PASS);
        assertTrue(diagnostics.hasGrpcResponse(results));
        assertFalse(diagnostics.hasRequiredFailure(results));
    }

    @Test
    void shouldReportNonGrpcHttpResponsePattern() {
        var probe = new FakeDiagnosticProbe();
        probe.grpcResult = new AviatorGrpcReachabilityResult(false, true, "UNAVAILABLE", "non-grpc-http-response", "503",
            "text/html", "HTTP status code 503 invalid content-type: text/html");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose("aviator.example.com", 5, "url");

        assertEquals(5, results.size());
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.FAIL);
        assertEquals("http-response-not-grpc", results.get(4).evidence().path("pattern").asText());
        assertEquals("503", results.get(4).evidence().path("httpStatusCode").asText());
        assertEquals("text/html", results.get(4).evidence().path("httpContentType").asText());
        assertFalse(diagnostics.hasGrpcResponse(results));
        assertTrue(diagnostics.hasRequiredFailure(results));
    }

    private static void assertStage(AviatorDiagnosticStageResult result, AviatorDiagnosticStage stage, AviatorDiagnosticStatus status) {
        assertEquals(stage, result.stage());
        assertEquals(status, result.status());
    }

    private static final class FakeDiagnosticProbe implements AviatorDiagnosticProbe {
        private IOException resolveException;
        private AviatorTlsHandshakeResult tlsResult = new AviatorTlsHandshakeResult(
            "TLSv1.3", "TLS_AES_128_GCM_SHA256", "CN=aviator.example.com", "h2", "not-used");
        private AviatorGrpcReachabilityResult grpcResult = new AviatorGrpcReachabilityResult(true, "OK", "response received");
        private boolean resolveCalled;
        private boolean connectCalled;
        private boolean handshakeCalled;
        private boolean grpcCalled;

        @Override
        public InetAddress[] resolve(String host) throws IOException {
            resolveCalled = true;
            if (resolveException != null) {
                throw resolveException;
            }
            return new InetAddress[] {InetAddress.getByName("127.0.0.1")};
        }

        @Override
        public void connect(String host, int port, int timeoutSeconds) {
            connectCalled = true;
        }

        @Override
        public AviatorTlsHandshakeResult handshake(AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
            handshakeCalled = true;
            return tlsResult;
        }

        @Override
        public AviatorGrpcReachabilityResult probeGrpc(String url, int timeoutSeconds) {
            grpcCalled = true;
            return grpcResult;
        }
    }
}