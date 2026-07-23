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
import java.util.Optional;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.ParsedTarget;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;

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
        assertFalse(probe.tunnelCalled);
    }

    @Test
    void shouldSkipTransportStagesWhenDnsFails() {
        var probe = new FakeDiagnosticProbe();
        probe.resolveException = new UnknownHostException("host not found");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planNoProxy(), 5, "url");

        assertEquals(5, results.size());
        assertStage(results.get(1), AviatorDiagnosticStage.DNS, AviatorDiagnosticStatus.FAIL);
        assertStage(results.get(2), AviatorDiagnosticStage.TCP, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.WARN);
        assertFalse(probe.tunnelCalled);
        assertFalse(probe.grpcCalled);
    }

    @Test
    void shouldReportGrpcNoResponsePattern() {
        var probe = new FakeDiagnosticProbe();
        probe.grpcResult = AviatorGrpcReachabilityResult.noResponse("DEADLINE_EXCEEDED",
            AviatorGrpcFailureCategory.NO_RESPONSE, "deadline exceeded");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planNoProxy(), 5, "url");

        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.PASS);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.FAIL);
        assertEquals(AviatorGrpcPattern.GRPC_NO_RESPONSE.wireId(),
            results.get(4).evidence().path("pattern").asText());
        assertFalse(diagnostics.hasGrpcResponse(results));
    }

    @Test
    void shouldReportGrpcTlsFailureNotAsTlsEstablishedNoResponse() {
        var probe = new FakeDiagnosticProbe();
        probe.grpcResult = AviatorGrpcReachabilityResult.noResponse("UNAVAILABLE", AviatorGrpcFailureCategory.TLS,
            "SSLHandshakeException");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planNoProxy(), 5, "url");

        assertEquals(AviatorGrpcPattern.GRPC_TLS_FAILED.wireId(), results.get(4).evidence().path("pattern").asText());
    }

    @Test
    void shouldContinueAfterTlsAlpnWarningWhenGrpcResponds() {
        var probe = new FakeDiagnosticProbe();
        probe.tunnelResult = new AviatorTunnelResult.TlsSucceeded(false, "not-used",
            "TLSv1.3", "TLS_AES_128_GCM_SHA256", "CN=aviator.example.com", "");
        probe.grpcResult = AviatorGrpcReachabilityResult.ok("UNAUTHENTICATED", "token required");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planNoProxy(), 5, "url");

        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.PASS);
        assertTrue(diagnostics.hasGrpcResponse(results));
    }

    @Test
    void shouldReportNonGrpcHttpResponsePattern() {
        var probe = new FakeDiagnosticProbe();
        probe.grpcResult = AviatorGrpcReachabilityResult.nonGrpcHttp("UNAVAILABLE", "503", "text/html",
            "HTTP status code 503 invalid content-type: text/html");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planNoProxy(), 5, "url");

        assertEquals(AviatorGrpcPattern.HTTP_RESPONSE_NOT_GRPC.wireId(),
            results.get(4).evidence().path("pattern").asText());
        assertEquals("503", results.get(4).evidence().path("httpStatusCode").asText());
    }

    @Test
    void shouldPassAllTransportStagesWithoutProxy() {
        var probe = new FakeDiagnosticProbe();
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planNoProxy(), 5, "url");

        assertEquals(5, results.size());
        assertStage(results.get(0), AviatorDiagnosticStage.ENDPOINT, AviatorDiagnosticStatus.PASS);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.PASS);
        assertTrue(probe.tunnelCalled);
        assertFalse(diagnostics.hasRequiredFailure(results));
    }

    @Test
    void shouldSkipTlsAndGrpcWhenTcpFails() {
        var probe = new FakeDiagnosticProbe();
        probe.connectException = new IOException("Connection refused");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planNoProxy("127.0.0.1", 1), 3, "url");

        assertStage(results.get(2), AviatorDiagnosticStage.TCP, AviatorDiagnosticStatus.FAIL);
        assertEquals("aviator", results.get(2).evidence().path("nextHopType").asText());
        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.WARN);
        assertFalse(probe.tunnelCalled);
    }

    @Test
    void shouldSkipGrpcWhenTlsFails() {
        var probe = new FakeDiagnosticProbe();
        probe.tunnelResult = new AviatorTunnelResult.TlsFailed(false, "not-used", AviatorTlsPhase.HANDSHAKE,
            new SSLHandshakeException("PKIX path building failed"));
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planNoProxy(), 5, "url");

        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.FAIL);
        assertEquals(AviatorTlsPhase.HANDSHAKE.id(), results.get(3).evidence().path("tlsPhase").asText());
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.WARN);
        assertFalse(probe.grpcCalled);
    }

    @Test
    void shouldLabelConnectPhaseWhenOpenFails() {
        var probe = new FakeDiagnosticProbe();
        probe.tunnelResult = new AviatorTunnelResult.TlsFailed(false, "not-used", AviatorTlsPhase.CONNECT,
            new IOException("Connection refused"));
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planNoProxy(), 5, "url");

        assertEquals(AviatorTlsPhase.CONNECT.id(), results.get(3).evidence().path("tlsPhase").asText());
        assertTrue(results.get(3).summary().toLowerCase().contains("connection"));
    }

    @Test
    void shouldIncludeProxyStageFromSingleTunnelSession() {
        var probe = new FakeDiagnosticProbe();
        probe.tunnelResult = new AviatorTunnelResult.TlsSucceeded(true, "HTTP/1.1 200 Connection established",
            "TLSv1.3", "TLS_AES_128_GCM_SHA256", "CN=aviator.example.com", "h2");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planWithProxy(), 5, "url");

        assertEquals(6, results.size());
        assertStage(results.get(2), AviatorDiagnosticStage.TCP, AviatorDiagnosticStatus.PASS);
        assertEquals("proxy", results.get(2).evidence().path("nextHopType").asText());
        assertStage(results.get(3), AviatorDiagnosticStage.PROXY, AviatorDiagnosticStatus.PASS);
        assertStage(results.get(4), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.PASS);
        assertStage(results.get(5), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.PASS);
        assertEquals(1, probe.tunnelCallCount);
    }

    @Test
    void shouldSkipTlsAndGrpcWhenProxyConnectFailsInTunnel() {
        var probe = new FakeDiagnosticProbe();
        probe.tunnelResult = new AviatorTunnelResult.ProxyConnectFailed(
            new AviatorProxyConnectException("Proxy CONNECT failed: HTTP/1.1 403"));
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planWithProxy(), 5, "url");

        assertEquals(6, results.size());
        assertStage(results.get(3), AviatorDiagnosticStage.PROXY, AviatorDiagnosticStatus.FAIL);
        assertStage(results.get(4), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(5), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.WARN);
        assertFalse(probe.grpcCalled);
        assertEquals(1, probe.tunnelCallCount);
    }

    @Test
    void shouldSkipProxyWhenTcpToProxyFails() {
        var probe = new FakeDiagnosticProbe();
        probe.connectException = new IOException("Connection refused");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planWithProxy(), 3, "url");

        assertEquals(6, results.size());
        assertStage(results.get(2), AviatorDiagnosticStage.TCP, AviatorDiagnosticStatus.FAIL);
        assertEquals("proxy", results.get(2).evidence().path("nextHopType").asText());
        assertStage(results.get(3), AviatorDiagnosticStage.PROXY, AviatorDiagnosticStatus.WARN);
        assertFalse(probe.tunnelCalled);
    }

    @Test
    void shouldTreatApplicationGrpcErrorAsResponseReceived() {
        var probe = new FakeDiagnosticProbe();
        probe.grpcResult = AviatorGrpcReachabilityResult.ok("INVALID_ARGUMENT", "invalid argument");
        var diagnostics = new AviatorConnectionDiagnostics(probe);

        var results = diagnostics.diagnose(planNoProxy(), 5, "url");

        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.PASS);
        assertTrue(diagnostics.hasGrpcResponse(results));
    }

    private static AviatorConnectionPlan planNoProxy() {
        return planNoProxy("aviator.example.com", 443);
    }

    private static AviatorConnectionPlan planNoProxy(String host, int port) {
        Integer targetPort = port == 443 ? null : port;
        return new AviatorConnectionPlan(host, "https://" + host,
            new ParsedTarget(host, targetPort), port, Optional.empty());
    }

    private static AviatorConnectionPlan planWithProxy() {
        var proxy = ProxyDescriptor.builder().proxyHost("proxy.example.com").proxyPort(8080).build();
        return new AviatorConnectionPlan("aviator.example.com", "https://aviator.example.com",
            new ParsedTarget("aviator.example.com", null), 443, Optional.of(proxy));
    }

    private static void assertStage(AviatorDiagnosticStageResult result, AviatorDiagnosticStage stage,
            AviatorDiagnosticStatus status) {
        assertEquals(stage, result.stage());
        assertEquals(status, result.status());
    }

    private static final class FakeDiagnosticProbe implements IAviatorDiagnosticProbe {
        private IOException resolveException;
        private IOException connectException;
        private AviatorTunnelResult tunnelResult = new AviatorTunnelResult.TlsSucceeded(false, "not-used",
            "TLSv1.3", "TLS_AES_128_GCM_SHA256", "CN=aviator.example.com", "h2");
        private AviatorGrpcReachabilityResult grpcResult = AviatorGrpcReachabilityResult.ok("OK", "response received");
        private boolean tunnelCalled;
        private int tunnelCallCount;
        private boolean grpcCalled;

        @Override
        public InetAddress[] resolve(String host) throws IOException {
            if (resolveException != null) {
                throw resolveException;
            }
            return new InetAddress[] {InetAddress.getByName("127.0.0.1")};
        }

        @Override
        public void connect(String host, int port, int timeoutSeconds) throws IOException {
            if (connectException != null) {
                throw connectException;
            }
        }

        @Override
        public AviatorTunnelResult probeTunnel(AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
            tunnelCalled = true;
            tunnelCallCount++;
            return tunnelResult;
        }

        @Override
        public AviatorGrpcReachabilityResult probeGrpc(String url, int timeoutSeconds) {
            grpcCalled = true;
            return grpcResult;
        }
    }
}
