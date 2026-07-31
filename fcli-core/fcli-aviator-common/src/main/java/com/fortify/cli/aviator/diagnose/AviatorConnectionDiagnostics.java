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

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.exception.AviatorBugException;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;
import com.fortify.cli.common.json.JsonHelper;

public class AviatorConnectionDiagnostics {
    private static final List<AviatorDiagnosticStage> TRANSPORT_STAGES = List.of(
        AviatorDiagnosticStage.DNS,
        AviatorDiagnosticStage.TCP,
        AviatorDiagnosticStage.PROXY,
        AviatorDiagnosticStage.TLS,
        AviatorDiagnosticStage.GRPC);

    private final IAviatorDiagnosticProbe probe;

    public AviatorConnectionDiagnostics() {
        this(new AviatorDefaultDiagnosticProbe());
    }

    /** Visible for tests and product helpers that inject a probe. */
    public AviatorConnectionDiagnostics(IAviatorDiagnosticProbe probe) {
        this.probe = probe;
    }

    public AviatorDiagnosticReport diagnose(String url, int timeoutSeconds, String sourceType) {
        try {
            return diagnose(AviatorGrpcClientHelper.createConnectionPlan(url), timeoutSeconds, sourceType);
        } catch (AviatorSimpleException e) {
            var report = new AviatorDiagnosticReport();
            report.fail(AviatorDiagnosticStage.ENDPOINT,
                "Endpoint is invalid", "Use a valid Aviator host name and optional port",
                AviatorDiagnosticEvidence.errorEvidence(e));
            skipAfter(report, null, AviatorDiagnosticStage.ENDPOINT, "endpoint configuration failed");
            return report;
        }
    }

    /**
     * Run diagnostics for a pre-built connection plan (tests inject plans to avoid ambient proxy env).
     */
    public AviatorDiagnosticReport diagnose(AviatorConnectionPlan connectionPlan, int timeoutSeconds, String sourceType) {
        var report = new AviatorDiagnosticReport();
        report.pass(AviatorDiagnosticStage.ENDPOINT,
            "Endpoint is valid", "No action required", endpointEvidence(connectionPlan, sourceType));

        if (!runDns(report, connectionPlan)) {
            skipAfter(report, connectionPlan, AviatorDiagnosticStage.DNS, "DNS resolution failed");
            return report;
        }
        if (!runTcp(report, connectionPlan, timeoutSeconds)) {
            skipAfter(report, connectionPlan, AviatorDiagnosticStage.TCP, "TCP connectivity failed");
            return report;
        }
        if (!runTunnelStages(report, connectionPlan, timeoutSeconds)) {
            return report;
        }
        runGrpc(report, connectionPlan, timeoutSeconds);
        return report;
    }

    private boolean runDns(AviatorDiagnosticReport report, AviatorConnectionPlan connectionPlan) {
        try {
            var evidence = JsonHelper.getObjectMapper().createObjectNode();
            addAddresses(evidence, "resolvedAddresses", probe.resolve(connectionPlan.target().host()));
            if (connectionPlan.proxyDescriptor().isPresent()) {
                var proxy = connectionPlan.proxyDescriptor().get();
                addAddresses(evidence, "proxyResolvedAddresses", probe.resolve(proxy.getProxyHost()));
            }
            report.pass(AviatorDiagnosticStage.DNS, "Host name resolved", "No action required", evidence);
            return true;
        } catch (IOException e) {
            report.fail(AviatorDiagnosticStage.DNS,
                "DNS resolution failed", "Check the Aviator host name, DNS, VPN, or proxy settings",
                AviatorDiagnosticEvidence.errorEvidence(e));
            return false;
        }
    }

    /**
     * Probes TCP to the next hop (proxy or Aviator host) and closes the socket.
     * <p>
     * Intentional second open: {@link #runTunnelStages} opens a new connection for
     * CONNECT/TLS so a pure next-hop TCP failure stays distinct from proxy CONNECT or TLS
     * handshake failure. Do not fold TCP into the tunnel solely to avoid a double connect.
     */
    private boolean runTcp(AviatorDiagnosticReport report, AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
        var proxyDescriptor = connectionPlan.proxyDescriptor();
        var nextHopHost = proxyDescriptor.map(proxy -> proxy.getProxyHost()).orElse(connectionPlan.target().host());
        var nextHopPort = proxyDescriptor.map(proxy -> proxy.getProxyPort()).orElse(connectionPlan.effectivePort());
        var evidence = nextHopEvidence(nextHopHost, nextHopPort, proxyDescriptor.isPresent());
        try {
            probe.connect(nextHopHost, nextHopPort, timeoutSeconds);
            report.pass(AviatorDiagnosticStage.TCP, "TCP connection opened", "No action required", evidence);
            return true;
        } catch (Exception e) {
            putError(evidence, e);
            var guidance = proxyDescriptor.isPresent()
                    ? "Check proxy host, port, credentials, and firewall access"
                    : "Check firewall, VPN, proxy, and port 443 access";
            report.fail(AviatorDiagnosticStage.TCP, "TCP connection failed", guidance, evidence);
            return false;
        }
    }

    /**
     * PROXY + TLS from one tunnel session (single CONNECT when proxy is configured).
     * Runs after the TCP stage, which already opened and closed a next-hop probe socket
     * so stage failures remain isolated (see {@link #runTcp}).
     *
     * @return true if TLS stage continued the pipeline (pass or alpn warn)
     */
    private boolean runTunnelStages(AviatorDiagnosticReport report, AviatorConnectionPlan connectionPlan,
            int timeoutSeconds) {
        var tunnel = probe.probeTunnel(connectionPlan, timeoutSeconds);
        if (tunnel instanceof AviatorTunnelResult.ProxyConnectFailed failed) {
            appendProxyFailure(report, connectionPlan, failed);
            skipAfter(report, connectionPlan, AviatorDiagnosticStage.PROXY, "proxy CONNECT failed");
            return false;
        }
        appendProxyPassIfConfigured(report, connectionPlan, tunnel);
        if (tunnel instanceof AviatorTunnelResult.TlsFailed failed) {
            appendTlsFailure(report, connectionPlan, failed);
            skipAfter(report, connectionPlan, AviatorDiagnosticStage.TLS, "TLS handshake failed");
            return false;
        }
        if (tunnel instanceof AviatorTunnelResult.TlsSucceeded ok) {
            appendTlsSuccess(report, ok);
            return true;
        }
        throw new AviatorBugException("Unhandled tunnel result: " + tunnel);
    }

    private void appendProxyFailure(AviatorDiagnosticReport report, AviatorConnectionPlan connectionPlan,
            AviatorTunnelResult.ProxyConnectFailed failed) {
        var evidence = AviatorDiagnosticEvidence.errorEvidence(failed.error());
        putProxyEvidence(evidence, connectionPlan);
        report.fail(AviatorDiagnosticStage.PROXY, "Proxy CONNECT failed",
            "Check proxy host, port, credentials, CONNECT allow-list, and authentication method", evidence);
    }

    private void appendProxyPassIfConfigured(AviatorDiagnosticReport report,
            AviatorConnectionPlan connectionPlan, AviatorTunnelResult tunnel) {
        if (connectionPlan.proxyDescriptor().isEmpty()) {
            return;
        }
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        evidence.put("proxyConnectStatus", tunnel.proxyConnectStatus());
        putProxyEvidence(evidence, connectionPlan);
        report.pass(AviatorDiagnosticStage.PROXY, "Proxy CONNECT succeeded", "No action required", evidence);
    }

    private void appendTlsSuccess(AviatorDiagnosticReport report, AviatorTunnelResult.TlsSucceeded ok) {
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        evidence.put("tlsProtocol", ok.protocol());
        evidence.put("tlsCipherSuite", ok.cipherSuite());
        evidence.put("tlsPeerSubject", ok.peerSubject());
        evidence.put("tlsAlpnProtocol", ok.applicationProtocol());
        evidence.put("proxyConnectStatus", ok.proxyConnectStatusLine());
        evidence.put("tlsPhase", AviatorTlsPhase.HANDSHAKE.id());
        if (!"h2".equals(ok.applicationProtocol())) {
            report.warn(AviatorDiagnosticStage.TLS,
                "TLS works, but HTTP/2 was not enabled",
                "Allow ALPN h2 through the proxy or gateway to aviator-grpc-server", true, evidence);
        } else {
            report.pass(AviatorDiagnosticStage.TLS, "TLS and HTTP/2 are available", "No action required", evidence);
        }
    }

    private void appendTlsFailure(AviatorDiagnosticReport report, AviatorConnectionPlan connectionPlan,
            AviatorTunnelResult.TlsFailed failed) {
        var evidence = AviatorDiagnosticEvidence.errorEvidence(failed.error());
        evidence.put("tlsPhase", failed.phase().id());
        if (failed.proxyConnectStatusLine() != null) {
            evidence.put("proxyConnectStatus", failed.proxyConnectStatusLine());
        }
        putProxyEvidence(evidence, connectionPlan);
        if (failed.phase() == AviatorTlsPhase.HANDSHAKE) {
            report.fail(AviatorDiagnosticStage.TLS, "TLS handshake failed",
                "Check certificate trust, SNI, and TLS inspection settings", evidence);
            return;
        }
        var guidance = connectionPlan.proxyDescriptor().isPresent()
                ? "Check proxy host, port, credentials, and firewall access to the proxy"
                : "Check firewall, VPN, proxy, and port access to the Aviator host";
        report.fail(AviatorDiagnosticStage.TLS, "Could not open connection for TLS probe", guidance, evidence);
    }

    private void runGrpc(AviatorDiagnosticReport report, AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
        try {
            applyGrpc(report, probe.probeGrpc(connectionPlan.originalUrl(), timeoutSeconds));
        } catch (Exception e) {
            applyGrpc(report, AviatorGrpcReachabilityResult.probeError(e));
        }
    }

    private void applyGrpc(AviatorDiagnosticReport report, AviatorGrpcReachabilityResult grpc) {
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        grpc.putEvidence(evidence);
        if (grpc.pattern() != null) {
            evidence.put("pattern", grpc.pattern().wireId());
        }
        if (grpc.stagePass()) {
            report.pass(AviatorDiagnosticStage.GRPC, grpc.stageSummary(), grpc.stageGuidance(), evidence);
        } else {
            report.fail(AviatorDiagnosticStage.GRPC, grpc.stageSummary(), grpc.stageGuidance(), evidence);
        }
    }

    private ObjectNode endpointEvidence(AviatorConnectionPlan connectionPlan, String sourceType) {
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        evidence.put("sourceType", sourceType);
        evidence.put("targetHost", connectionPlan.target().host());
        evidence.put("targetPort", connectionPlan.effectivePort());
        evidence.put("normalizedUrl", connectionPlan.normalizedUrl());
        putProxyEvidence(evidence, connectionPlan);
        return evidence;
    }

    private static ObjectNode nextHopEvidence(String host, int port, boolean proxy) {
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        evidence.put("nextHopHost", host);
        evidence.put("nextHopPort", port);
        evidence.put("nextHopType", proxy ? "proxy" : "aviator");
        return evidence;
    }

    private static void putProxyEvidence(ObjectNode evidence, AviatorConnectionPlan connectionPlan) {
        connectionPlan.proxyDescriptor().ifPresent(proxy -> {
            evidence.put("proxyHost", proxy.getProxyHost());
            evidence.put("proxyPort", proxy.getProxyPort());
            evidence.put("proxyAuthConfigured", proxy.getProxyUser() != null);
        });
    }

    private static void putError(ObjectNode evidence, Exception e) {
        AviatorDiagnosticEvidence.merge(evidence, AviatorDiagnosticEvidence.errorEvidence(e));
    }

    /**
     * Append WARN skips for every transport stage strictly after {@code failedStage}.
     * PROXY is omitted when the plan has no proxy (or plan is null after endpoint failure).
     */
    private static void skipAfter(AviatorDiagnosticReport report, AviatorConnectionPlan connectionPlan,
            AviatorDiagnosticStage failedStage, String reason) {
        var hasProxy = connectionPlan != null && connectionPlan.proxyDescriptor().isPresent();
        var afterFailed = failedStage == AviatorDiagnosticStage.ENDPOINT;
        for (var stage : TRANSPORT_STAGES) {
            if (!afterFailed) {
                if (stage == failedStage) {
                    afterFailed = true;
                }
                continue;
            }
            if (stage == AviatorDiagnosticStage.PROXY && !hasProxy) {
                continue;
            }
            report.skipWarn(stage, "Skipped because " + reason,
                "Resolve the previous failed required stage first",
                AviatorDiagnosticEvidence.empty());
        }
    }

    private static void addAddresses(ObjectNode evidence, String fieldName, InetAddress[] addresses) {
        var array = evidence.putArray(fieldName);
        Arrays.stream(addresses).map(InetAddress::getHostAddress).forEach(array::add);
    }
}
