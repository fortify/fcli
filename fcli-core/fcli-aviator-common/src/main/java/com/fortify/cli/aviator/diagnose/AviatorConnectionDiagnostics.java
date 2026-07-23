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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
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

    public List<AviatorDiagnosticStageResult> diagnose(String url, int timeoutSeconds, String sourceType) {
        try {
            return diagnose(AviatorGrpcClientHelper.createConnectionPlan(url), timeoutSeconds, sourceType);
        } catch (AviatorSimpleException e) {
            var results = new ArrayList<AviatorDiagnosticStageResult>();
            results.add(AviatorDiagnosticStageResult.fail(nextOrder(results), AviatorDiagnosticStage.ENDPOINT,
                "Endpoint is invalid", "Use a valid Aviator host name and optional port", errorEvidence(e)));
            skipAfter(results, null, AviatorDiagnosticStage.ENDPOINT, "endpoint configuration failed");
            return results;
        }
    }

    /**
     * Run diagnostics for a pre-built connection plan (tests inject plans to avoid ambient proxy env).
     */
    public List<AviatorDiagnosticStageResult> diagnose(AviatorConnectionPlan connectionPlan, int timeoutSeconds, String sourceType) {
        var results = new ArrayList<AviatorDiagnosticStageResult>();
        results.add(AviatorDiagnosticStageResult.pass(nextOrder(results), AviatorDiagnosticStage.ENDPOINT,
            "Endpoint is valid", "No action required", endpointEvidence(connectionPlan, sourceType)));

        if (!runDns(results, connectionPlan)) {
            skipAfter(results, connectionPlan, AviatorDiagnosticStage.DNS, "DNS resolution failed");
            return results;
        }
        if (!runTcp(results, connectionPlan, timeoutSeconds)) {
            skipAfter(results, connectionPlan, AviatorDiagnosticStage.TCP, "TCP connectivity failed");
            return results;
        }
        if (!runTunnelStages(results, connectionPlan, timeoutSeconds)) {
            return results;
        }
        runGrpc(results, connectionPlan, timeoutSeconds);
        return results;
    }

    public ArrayNode toArrayNode(List<AviatorDiagnosticStageResult> results) {
        var array = JsonHelper.getObjectMapper().createArrayNode();
        results.stream().map(AviatorDiagnosticStageResult::asObjectNode).forEach(array::add);
        return array;
    }

    public boolean hasRequiredFailure(List<AviatorDiagnosticStageResult> results) {
        return results.stream().anyMatch(AviatorDiagnosticStageResult::isRequiredFailure);
    }

    /**
     * True when the gRPC stage completed with a server response (stage status PASS).
     * Uses typed stage status rather than evidence JSON keys.
     */
    public boolean hasGrpcResponse(List<AviatorDiagnosticStageResult> results) {
        return results.stream()
            .anyMatch(result -> AviatorDiagnosticStage.GRPC.equals(result.stage())
                    && AviatorDiagnosticStatus.PASS.equals(result.status()));
    }

    private boolean runDns(List<AviatorDiagnosticStageResult> results, AviatorConnectionPlan connectionPlan) {
        try {
            var evidence = JsonHelper.getObjectMapper().createObjectNode();
            addAddresses(evidence, "resolvedAddresses", probe.resolve(connectionPlan.target().host()));
            if (connectionPlan.proxyDescriptor().isPresent()) {
                var proxy = connectionPlan.proxyDescriptor().get();
                addAddresses(evidence, "proxyResolvedAddresses", probe.resolve(proxy.getProxyHost()));
            }
            results.add(AviatorDiagnosticStageResult.pass(nextOrder(results), AviatorDiagnosticStage.DNS,
                "Host name resolved", "No action required", evidence));
            return true;
        } catch (IOException e) {
            results.add(AviatorDiagnosticStageResult.fail(nextOrder(results), AviatorDiagnosticStage.DNS,
                "DNS resolution failed", "Check the Aviator host name, DNS, VPN, or proxy settings", errorEvidence(e)));
            return false;
        }
    }

    private boolean runTcp(List<AviatorDiagnosticStageResult> results, AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
        var proxyDescriptor = connectionPlan.proxyDescriptor();
        var nextHopHost = proxyDescriptor.map(proxy -> proxy.getProxyHost()).orElse(connectionPlan.target().host());
        var nextHopPort = proxyDescriptor.map(proxy -> proxy.getProxyPort()).orElse(connectionPlan.effectivePort());
        var evidence = nextHopEvidence(nextHopHost, nextHopPort, proxyDescriptor.isPresent());
        try {
            probe.connect(nextHopHost, nextHopPort, timeoutSeconds);
            results.add(AviatorDiagnosticStageResult.pass(nextOrder(results), AviatorDiagnosticStage.TCP,
                "TCP connection opened", "No action required", evidence));
            return true;
        } catch (Exception e) {
            putError(evidence, e);
            var guidance = proxyDescriptor.isPresent()
                    ? "Check proxy host, port, credentials, and firewall access"
                    : "Check firewall, VPN, proxy, and port 443 access";
            results.add(AviatorDiagnosticStageResult.fail(nextOrder(results), AviatorDiagnosticStage.TCP,
                "TCP connection failed", guidance, evidence));
            return false;
        }
    }

    /**
     * PROXY + TLS from one tunnel session (single CONNECT when proxy is configured).
     * @return true if TLS stage continued the pipeline (pass or alpn warn)
     */
    private boolean runTunnelStages(List<AviatorDiagnosticStageResult> results, AviatorConnectionPlan connectionPlan,
            int timeoutSeconds) {
        var tunnel = probe.probeTunnel(connectionPlan, timeoutSeconds);
        if (tunnel instanceof AviatorTunnelResult.ProxyConnectFailed failed) {
            appendProxyFailure(results, connectionPlan, failed);
            skipAfter(results, connectionPlan, AviatorDiagnosticStage.PROXY, "proxy CONNECT failed");
            return false;
        }
        appendProxyPassIfConfigured(results, connectionPlan, tunnel);
        if (tunnel instanceof AviatorTunnelResult.TlsFailed failed) {
            appendTlsFailure(results, connectionPlan, failed);
            skipAfter(results, connectionPlan, AviatorDiagnosticStage.TLS, "TLS handshake failed");
            return false;
        }
        if (tunnel instanceof AviatorTunnelResult.TlsSucceeded ok) {
            appendTlsSuccess(results, ok);
            return true;
        }
        throw new AviatorBugException("Unhandled tunnel result: " + tunnel);
    }

    private void appendProxyFailure(List<AviatorDiagnosticStageResult> results, AviatorConnectionPlan connectionPlan,
            AviatorTunnelResult.ProxyConnectFailed failed) {
        var evidence = errorEvidence(failed.error());
        putProxyEvidence(evidence, connectionPlan);
        results.add(AviatorDiagnosticStageResult.fail(nextOrder(results), AviatorDiagnosticStage.PROXY,
            "Proxy CONNECT failed",
            "Check proxy host, port, credentials, CONNECT allow-list, and authentication method", evidence));
    }

    private void appendProxyPassIfConfigured(List<AviatorDiagnosticStageResult> results,
            AviatorConnectionPlan connectionPlan, AviatorTunnelResult tunnel) {
        if (connectionPlan.proxyDescriptor().isEmpty()) {
            return;
        }
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        evidence.put("proxyConnectStatus", proxyStatusFrom(tunnel));
        putProxyEvidence(evidence, connectionPlan);
        results.add(AviatorDiagnosticStageResult.pass(nextOrder(results), AviatorDiagnosticStage.PROXY,
            "Proxy CONNECT succeeded", "No action required", evidence));
    }

    private void appendTlsSuccess(List<AviatorDiagnosticStageResult> results, AviatorTunnelResult.TlsSucceeded ok) {
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        evidence.put("tlsProtocol", ok.protocol());
        evidence.put("tlsCipherSuite", ok.cipherSuite());
        evidence.put("tlsPeerSubject", ok.peerSubject());
        evidence.put("tlsAlpnProtocol", ok.applicationProtocol());
        evidence.put("proxyConnectStatus", ok.proxyConnectStatus());
        evidence.put("tlsPhase", AviatorTlsPhase.HANDSHAKE.id());
        if (!"h2".equals(ok.applicationProtocol())) {
            results.add(AviatorDiagnosticStageResult.warn(nextOrder(results), AviatorDiagnosticStage.TLS,
                "TLS works, but HTTP/2 was not enabled",
                "Allow ALPN h2 through the proxy or gateway to aviator-grpc-server", true, evidence));
        } else {
            results.add(AviatorDiagnosticStageResult.pass(nextOrder(results), AviatorDiagnosticStage.TLS,
                "TLS and HTTP/2 are available", "No action required", evidence));
        }
    }

    private void appendTlsFailure(List<AviatorDiagnosticStageResult> results, AviatorConnectionPlan connectionPlan,
            AviatorTunnelResult.TlsFailed failed) {
        var evidence = errorEvidence(failed.error());
        evidence.put("tlsPhase", failed.phase().id());
        if (failed.proxyConnectStatus() != null) {
            evidence.put("proxyConnectStatus", failed.proxyConnectStatus());
        }
        putProxyEvidence(evidence, connectionPlan);
        if (failed.phase() == AviatorTlsPhase.HANDSHAKE) {
            results.add(AviatorDiagnosticStageResult.fail(nextOrder(results), AviatorDiagnosticStage.TLS,
                "TLS handshake failed",
                "Check certificate trust, SNI, and TLS inspection settings", evidence));
            return;
        }
        var guidance = connectionPlan.proxyDescriptor().isPresent()
                ? "Check proxy host, port, credentials, and firewall access to the proxy"
                : "Check firewall, VPN, proxy, and port access to the Aviator host";
        results.add(AviatorDiagnosticStageResult.fail(nextOrder(results), AviatorDiagnosticStage.TLS,
            "Could not open connection for TLS probe", guidance, evidence));
    }

    private void runGrpc(List<AviatorDiagnosticStageResult> results, AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
        try {
            var grpc = probe.probeGrpc(connectionPlan.originalUrl(), timeoutSeconds);
            applyGrpcClassification(results, grpcEvidence(grpc), AviatorGrpcStageClassification.classify(grpc));
        } catch (Exception e) {
            var evidence = errorEvidence(e);
            evidence.put("grpcResponseReceived", false);
            applyGrpcClassification(results, evidence, AviatorGrpcStageClassification.classifyException(e));
        }
    }

    private void applyGrpcClassification(List<AviatorDiagnosticStageResult> results, ObjectNode evidence,
            AviatorGrpcStageClassification.Result classification) {
        if (classification.pattern() != null) {
            evidence.put("pattern", classification.pattern().wireId());
        }
        if (classification.pass()) {
            results.add(AviatorDiagnosticStageResult.pass(nextOrder(results), AviatorDiagnosticStage.GRPC,
                classification.summary(), classification.guidance(), evidence));
        } else {
            results.add(AviatorDiagnosticStageResult.fail(nextOrder(results), AviatorDiagnosticStage.GRPC,
                classification.summary(), classification.guidance(), evidence));
        }
    }

    private static ObjectNode grpcEvidence(AviatorGrpcReachabilityResult grpc) {
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        evidence.put("grpcResponseReceived", grpc.responseReceived());
        evidence.put("grpcStatusCode", grpc.statusCode());
        if (grpc.description() != null) {
            evidence.put("grpcDescription", grpc.description());
        }
        if (grpc.failureCategory() != null && grpc.failureCategory().wireId() != null) {
            evidence.put("failureCategory", grpc.failureCategory().wireId());
        }
        evidence.put("httpResponseReceived", grpc.httpResponseReceived());
        if (grpc.httpStatusCode() != null) {
            evidence.put("httpStatusCode", grpc.httpStatusCode());
        }
        if (grpc.httpContentType() != null) {
            evidence.put("httpContentType", grpc.httpContentType());
        }
        return evidence;
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
        var err = errorEvidence(e);
        err.fields().forEachRemaining(entry -> evidence.set(entry.getKey(), entry.getValue()));
    }

    private static String proxyStatusFrom(AviatorTunnelResult tunnel) {
        if (tunnel instanceof AviatorTunnelResult.TlsSucceeded s) {
            return s.proxyConnectStatus();
        }
        if (tunnel instanceof AviatorTunnelResult.TlsFailed f) {
            return f.proxyConnectStatus();
        }
        return "not-used";
    }

    /**
     * Append WARN skips for every transport stage strictly after {@code failedStage}.
     * PROXY is omitted when the plan has no proxy (or plan is null after endpoint failure).
     */
    private static void skipAfter(List<AviatorDiagnosticStageResult> results, AviatorConnectionPlan connectionPlan,
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
            results.add(AviatorDiagnosticStageResult.warn(nextOrder(results), stage,
                "Skipped because " + reason, "Resolve the previous failed required stage first", true,
                JsonHelper.getObjectMapper().createObjectNode()));
        }
    }

    private static void addAddresses(ObjectNode evidence, String fieldName, InetAddress[] addresses) {
        var array = evidence.putArray(fieldName);
        Arrays.stream(addresses).map(InetAddress::getHostAddress).forEach(array::add);
    }

    private static int nextOrder(List<AviatorDiagnosticStageResult> results) {
        return results.size() + 1;
    }

    public static ObjectNode errorEvidence(Exception e) {
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        evidence.put("exceptionType", e.getClass().getName());
        evidence.put("exceptionMessage", e.getMessage());
        var cause = e.getCause();
        if (cause != null) {
            evidence.put("causeType", cause.getClass().getName());
            evidence.put("causeMessage", cause.getMessage());
            var nested = cause.getCause();
            if (nested != null) {
                evidence.put("rootCauseType", nested.getClass().getName());
                evidence.put("rootCauseMessage", nested.getMessage());
            }
        }
        return evidence;
    }
}
