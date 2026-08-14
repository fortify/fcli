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
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator._common.exception.UnsupportedAviatorUrlSchemeException;
import com.fortify.cli.aviator.diagnose.support.ConfigurableDiagnosticProbe;
import com.fortify.cli.aviator.diagnose.support.OfflineConnectionPlan;

class AviatorConnectionDiagnosticsTest {
    @Test
    void shouldSkipDependentStagesWhenEndpointIsInvalid() {
        var probe = new ConfigurableDiagnosticProbe();
        var report = new AviatorConnectionDiagnostics(probe).diagnose(" ", 5, "url");
        var results = report.stages();

        assertEquals(5, results.size());
        assertStage(results.get(0), AviatorDiagnosticStage.ENDPOINT, AviatorDiagnosticStatus.FAIL);
        assertEquals("Endpoint is invalid", results.get(0).summary());
        assertStage(results.get(1), AviatorDiagnosticStage.DNS, AviatorDiagnosticStatus.WARN);
        assertTrue(results.get(1).summary().contains("endpoint validation failed"));
        assertStage(results.get(2), AviatorDiagnosticStage.TCP, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.WARN);
        assertTrue(report.hasRequiredFailure());
        assertFalse(probe.tunnelCalled);
    }

    @Test
    void shouldFailEndpointForUnsupportedUrlScheme() {
        var probe = new ConfigurableDiagnosticProbe();
        var report = new AviatorConnectionDiagnostics(probe).diagnose("mttp://vacant", 5, "url");
        var results = report.stages();

        assertEquals(5, results.size());
        assertStage(results.get(0), AviatorDiagnosticStage.ENDPOINT, AviatorDiagnosticStatus.FAIL);
        assertEquals(UnsupportedAviatorUrlSchemeException.STAGE_SUMMARY, results.get(0).summary());
        assertEquals(UnsupportedAviatorUrlSchemeException.STAGE_GUIDANCE, results.get(0).guidance());
        assertEquals("mttp", results.get(0).evidence().path("scheme").asText());
        assertEquals("mttp://vacant", results.get(0).evidence().path("providedUrl").asText());
        assertStage(results.get(1), AviatorDiagnosticStage.DNS, AviatorDiagnosticStatus.WARN);
        assertEquals("Skipped because endpoint validation failed", results.get(1).summary());
        assertStage(results.get(2), AviatorDiagnosticStage.TCP, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.WARN);
        assertTrue(report.hasRequiredFailure());
        assertFalse(probe.tunnelCalled);
        assertFalse(probe.grpcCalled);
    }

    @Test
    void shouldFailEndpointForFtpSchemeEvenWithValidLookingHost() {
        var probe = new ConfigurableDiagnosticProbe();
        var report = new AviatorConnectionDiagnostics(probe)
            .diagnose("ftp://aviator-qa01.example.com", 5, "url");

        assertStage(report.stages().get(0), AviatorDiagnosticStage.ENDPOINT, AviatorDiagnosticStatus.FAIL);
        assertEquals(UnsupportedAviatorUrlSchemeException.STAGE_SUMMARY, report.stages().get(0).summary());
        assertEquals("ftp", report.stages().get(0).evidence().path("scheme").asText());
        assertTrue(report.hasRequiredFailure());
        assertFalse(probe.tunnelCalled);
    }

    @Test
    void shouldFailEndpointForHttpScheme() {
        var probe = new ConfigurableDiagnosticProbe();
        var report = new AviatorConnectionDiagnostics(probe).diagnose("http://aviator.invalid", 5, "url");

        assertStage(report.stages().get(0), AviatorDiagnosticStage.ENDPOINT, AviatorDiagnosticStatus.FAIL);
        assertEquals(UnsupportedAviatorUrlSchemeException.STAGE_SUMMARY, report.stages().get(0).summary());
        assertEquals("http", report.stages().get(0).evidence().path("scheme").asText());
        assertFalse(probe.tunnelCalled);
    }

    @Test
    void shouldSkipTransportStagesWhenDnsFails() {
        var probe = new ConfigurableDiagnosticProbe();
        probe.resolveException = new UnknownHostException("host not found");
        var report = new AviatorConnectionDiagnostics(probe).diagnose(OfflineConnectionPlan.noProxy(), 5, "url");
        var results = report.stages();

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
        var probe = new ConfigurableDiagnosticProbe(
            AviatorGrpcReachabilityResult.noResponse("DEADLINE_EXCEEDED", "deadline exceeded"));
        var report = new AviatorConnectionDiagnostics(probe).diagnose(OfflineConnectionPlan.noProxy(), 5, "url");
        var results = report.stages();

        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.PASS);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.FAIL);
        assertEquals(AviatorGrpcPattern.GRPC_NO_RESPONSE.wireId(),
            results.get(4).evidence().path("pattern").asText());
        assertFalse(report.hasGrpcStagePass());
    }

    @Test
    void shouldContinueAfterTlsAlpnWarningWhenGrpcResponds() {
        var probe = new ConfigurableDiagnosticProbe(
            AviatorGrpcReachabilityResult.responseReceived("UNAUTHENTICATED", "token required"));
        probe.tunnelResult = new AviatorTunnelResult.TlsSucceeded(false, "not-used",
            "TLSv1.3", "TLS_AES_128_GCM_SHA256", "CN=aviator.invalid", "");
        var report = new AviatorConnectionDiagnostics(probe).diagnose(OfflineConnectionPlan.noProxy(), 5, "url");
        var results = report.stages();

        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.PASS);
        assertTrue(report.hasGrpcStagePass());
    }

    @Test
    void shouldReportNonGrpcHttpResponsePattern() {
        var probe = new ConfigurableDiagnosticProbe(AviatorGrpcReachabilityResult.nonGrpcHttp(
            "UNAVAILABLE", "503", "text/html", "HTTP status code 503 invalid content-type: text/html"));
        var results = new AviatorConnectionDiagnostics(probe)
            .diagnose(OfflineConnectionPlan.noProxy(), 5, "url").stages();

        assertEquals(AviatorGrpcPattern.HTTP_RESPONSE_NOT_GRPC.wireId(),
            results.get(4).evidence().path("pattern").asText());
        assertEquals("503", results.get(4).evidence().path("httpStatusCode").asText());
    }

    @Test
    void shouldPassAllTransportStagesWithoutProxy() {
        var probe = new ConfigurableDiagnosticProbe();
        var report = new AviatorConnectionDiagnostics(probe).diagnose(OfflineConnectionPlan.noProxy(), 5, "url");

        assertEquals(5, report.stages().size());
        assertStage(report.stages().get(0), AviatorDiagnosticStage.ENDPOINT, AviatorDiagnosticStatus.PASS);
        assertStage(report.stages().get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.PASS);
        assertTrue(probe.tunnelCalled);
        assertFalse(report.hasRequiredFailure());
    }

    @Test
    void shouldSkipTlsAndGrpcWhenTcpFails() {
        var probe = new ConfigurableDiagnosticProbe();
        probe.connectException = new IOException("Connection refused");
        var results = new AviatorConnectionDiagnostics(probe)
            .diagnose(OfflineConnectionPlan.noProxy("127.0.0.1", 1), 3, "url").stages();

        assertStage(results.get(2), AviatorDiagnosticStage.TCP, AviatorDiagnosticStatus.FAIL);
        assertEquals("aviator", results.get(2).evidence().path("nextHopType").asText());
        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.WARN);
        assertFalse(probe.tunnelCalled);
    }

    @Test
    void shouldSkipGrpcWhenTlsFails() {
        var probe = new ConfigurableDiagnosticProbe();
        probe.tunnelResult = new AviatorTunnelResult.TlsFailed(false, "not-used", AviatorTlsPhase.HANDSHAKE,
            new SSLHandshakeException("PKIX path building failed"));
        var results = new AviatorConnectionDiagnostics(probe)
            .diagnose(OfflineConnectionPlan.noProxy(), 5, "url").stages();

        assertStage(results.get(3), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.FAIL);
        assertEquals(AviatorTlsPhase.HANDSHAKE.id(), results.get(3).evidence().path("tlsPhase").asText());
        assertStage(results.get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.WARN);
        assertFalse(probe.grpcCalled);
    }

    @Test
    void shouldLabelConnectPhaseWhenOpenFails() {
        var probe = new ConfigurableDiagnosticProbe();
        probe.tunnelResult = new AviatorTunnelResult.TlsFailed(false, "not-used", AviatorTlsPhase.CONNECT,
            new IOException("Connection refused"));
        var results = new AviatorConnectionDiagnostics(probe)
            .diagnose(OfflineConnectionPlan.noProxy(), 5, "url").stages();

        assertEquals(AviatorTlsPhase.CONNECT.id(), results.get(3).evidence().path("tlsPhase").asText());
        assertTrue(results.get(3).summary().toLowerCase().contains("connection"));
    }

    @Test
    void shouldIncludeProxyStageFromSingleTunnelSession() {
        var probe = new ConfigurableDiagnosticProbe();
        probe.tunnelResult = new AviatorTunnelResult.TlsSucceeded(true, "HTTP/1.1 200 Connection established",
            "TLSv1.3", "TLS_AES_128_GCM_SHA256", "CN=aviator.invalid", "h2");
        var results = new AviatorConnectionDiagnostics(probe)
            .diagnose(OfflineConnectionPlan.withProxy(), 5, "url").stages();

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
        var probe = new ConfigurableDiagnosticProbe();
        probe.tunnelResult = new AviatorTunnelResult.ProxyConnectFailed(
            new AviatorProxyConnectException("Proxy CONNECT failed: HTTP/1.1 403"));
        var results = new AviatorConnectionDiagnostics(probe)
            .diagnose(OfflineConnectionPlan.withProxy(), 5, "url").stages();

        assertEquals(6, results.size());
        assertStage(results.get(3), AviatorDiagnosticStage.PROXY, AviatorDiagnosticStatus.FAIL);
        assertStage(results.get(4), AviatorDiagnosticStage.TLS, AviatorDiagnosticStatus.WARN);
        assertStage(results.get(5), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.WARN);
        assertFalse(probe.grpcCalled);
        assertEquals(1, probe.tunnelCallCount);
    }

    @Test
    void shouldSkipProxyWhenTcpToProxyFails() {
        var probe = new ConfigurableDiagnosticProbe();
        probe.connectException = new IOException("Connection refused");
        var results = new AviatorConnectionDiagnostics(probe)
            .diagnose(OfflineConnectionPlan.withProxy(), 3, "url").stages();

        assertEquals(6, results.size());
        assertStage(results.get(2), AviatorDiagnosticStage.TCP, AviatorDiagnosticStatus.FAIL);
        assertEquals("proxy", results.get(2).evidence().path("nextHopType").asText());
        assertStage(results.get(3), AviatorDiagnosticStage.PROXY, AviatorDiagnosticStatus.WARN);
        assertFalse(probe.tunnelCalled);
    }

    @Test
    void shouldTreatApplicationGrpcErrorAsResponseReceived() {
        var probe = new ConfigurableDiagnosticProbe(
            AviatorGrpcReachabilityResult.responseReceived("INVALID_ARGUMENT", "invalid argument"));
        var report = new AviatorConnectionDiagnostics(probe).diagnose(OfflineConnectionPlan.noProxy(), 5, "url");

        assertStage(report.stages().get(4), AviatorDiagnosticStage.GRPC, AviatorDiagnosticStatus.PASS);
        assertTrue(report.hasGrpcStagePass());
    }

    private static void assertStage(AviatorDiagnosticStageResult result, AviatorDiagnosticStage stage,
            AviatorDiagnosticStatus status) {
        assertTrue(result.isStage(stage), "expected stage " + stage.id() + " but was " + result.stage());
        assertEquals(status, result.status());
    }
}
