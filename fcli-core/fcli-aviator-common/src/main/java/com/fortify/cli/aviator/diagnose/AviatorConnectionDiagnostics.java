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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;
import com.fortify.cli.common.json.JsonHelper;

public class AviatorConnectionDiagnostics {
    private final AviatorDiagnosticProbe probe;

    public AviatorConnectionDiagnostics() {
        this(new AviatorDefaultDiagnosticProbe());
    }

    AviatorConnectionDiagnostics(AviatorDiagnosticProbe probe) {
        this.probe = probe;
    }

    public List<AviatorDiagnosticStageResult> diagnose(String url, int timeoutSeconds, String sourceType) {
        var results = new ArrayList<AviatorDiagnosticStageResult>();
        AviatorConnectionPlan connectionPlan;
        try {
            connectionPlan = AviatorGrpcClientHelper.createConnectionPlan(url);
            results.add(pass(1, AviatorDiagnosticStage.ENDPOINT, "Endpoint is valid",
                "No action required", endpointEvidence(connectionPlan, sourceType)));
        } catch (AviatorSimpleException e) {
            results.add(fail(1, AviatorDiagnosticStage.ENDPOINT, "Endpoint is invalid",
                "Use a valid Aviator host name and optional port", errorEvidence(e)));
            addSkipped(results, 2, AviatorDiagnosticStage.DNS, "Skipped because endpoint configuration failed");
            addSkipped(results, 3, AviatorDiagnosticStage.TCP, "Skipped because endpoint configuration failed");
            addSkipped(results, 4, AviatorDiagnosticStage.TLS, "Skipped because endpoint configuration failed");
            addSkipped(results, 5, AviatorDiagnosticStage.GRPC, "Skipped because endpoint configuration failed");
            return results;
        }

        if (!runDns(results, connectionPlan)) {
            addSkipped(results, 3, AviatorDiagnosticStage.TCP, "Skipped because DNS resolution failed");
            addSkipped(results, 4, AviatorDiagnosticStage.TLS, "Skipped because DNS resolution failed");
            addSkipped(results, 5, AviatorDiagnosticStage.GRPC, "Skipped because DNS resolution failed");
            return results;
        }
        if (!runTcp(results, connectionPlan, timeoutSeconds)) {
            addSkipped(results, 4, AviatorDiagnosticStage.TLS, "Skipped because TCP connectivity failed");
            addSkipped(results, 5, AviatorDiagnosticStage.GRPC, "Skipped because TCP connectivity failed");
            return results;
        }
        var tlsPassed = runTls(results, connectionPlan, timeoutSeconds);
        if (!tlsPassed) {
            addSkipped(results, 5, AviatorDiagnosticStage.GRPC, "Skipped because TLS handshake failed");
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

    public boolean hasGrpcResponse(List<AviatorDiagnosticStageResult> results) {
        return results.stream()
            .filter(result -> AviatorDiagnosticStage.GRPC.equals(result.stage()))
            .findFirst()
            .map(result -> result.evidence().path("grpcResponseReceived").asBoolean(false))
            .orElse(false);
    }

    private boolean runDns(List<AviatorDiagnosticStageResult> results, AviatorConnectionPlan connectionPlan) {
        try {
            var evidence = JsonHelper.getObjectMapper().createObjectNode();
            addAddresses(evidence, "resolvedAddresses", probe.resolve(connectionPlan.target().host()));
            if (connectionPlan.proxyDescriptor().isPresent()) {
                var proxy = connectionPlan.proxyDescriptor().get();
                addAddresses(evidence, "proxyResolvedAddresses", probe.resolve(proxy.getProxyHost()));
            }
            results.add(pass(2, AviatorDiagnosticStage.DNS, "Host name resolved",
                "No action required", evidence));
            return true;
        } catch (IOException e) {
            results.add(fail(2, AviatorDiagnosticStage.DNS, "DNS resolution failed",
                "Check the Aviator host name, DNS, VPN, or proxy settings", errorEvidence(e)));
            return false;
        }
    }

    private boolean runTcp(List<AviatorDiagnosticStageResult> results, AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
        var proxyDescriptor = connectionPlan.proxyDescriptor();
        var nextHopHost = proxyDescriptor.map(proxy -> proxy.getProxyHost()).orElse(connectionPlan.target().host());
        var nextHopPort = proxyDescriptor.map(proxy -> proxy.getProxyPort()).orElse(connectionPlan.effectivePort());
        try {
            probe.connect(nextHopHost, nextHopPort, timeoutSeconds);
            var evidence = JsonHelper.getObjectMapper().createObjectNode();
            evidence.put("nextHopHost", nextHopHost);
            evidence.put("nextHopPort", nextHopPort);
            evidence.put("nextHopType", proxyDescriptor.isPresent() ? "proxy" : "aviator");
            results.add(pass(3, AviatorDiagnosticStage.TCP, "TCP connection opened",
                "No action required", evidence));
            return true;
        } catch (Exception e) {
            var evidence = errorEvidence(e);
            evidence.put("nextHopHost", nextHopHost);
            evidence.put("nextHopPort", nextHopPort);
            evidence.put("nextHopType", proxyDescriptor.isPresent() ? "proxy" : "aviator");
            var guidance = proxyDescriptor.isPresent()
                    ? "Check proxy host, port, credentials, and firewall access"
                    : "Check firewall, VPN, proxy, and port 443 access";
                results.add(fail(3, AviatorDiagnosticStage.TCP, "TCP connection failed", guidance, evidence));
            return false;
        }
    }

    private boolean runTls(List<AviatorDiagnosticStageResult> results, AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
        try {
            var tls = probe.handshake(connectionPlan, timeoutSeconds);
            var evidence = JsonHelper.getObjectMapper().createObjectNode();
            evidence.put("tlsProtocol", tls.protocol());
            evidence.put("tlsCipherSuite", tls.cipherSuite());
            evidence.put("tlsPeerSubject", tls.peerSubject());
            evidence.put("tlsAlpnProtocol", tls.applicationProtocol());
            evidence.put("proxyConnectStatus", tls.proxyConnectStatus());
            if (!isHttp2Protocol(tls.applicationProtocol())) {
                results.add(warn(4, AviatorDiagnosticStage.TLS, "TLS works, but HTTP/2 was not enabled",
                    "Allow ALPN h2 through the proxy or gateway to aviator-grpc-server", true, evidence));
            } else {
                results.add(pass(4, AviatorDiagnosticStage.TLS, "TLS and HTTP/2 are available",
                    "No action required", evidence));
            }
            return true;
        } catch (Exception e) {
            results.add(fail(4, AviatorDiagnosticStage.TLS, "TLS handshake failed",
                "Check certificate trust, SNI, TLS inspection, and proxy CONNECT rules", errorEvidence(e)));
            return false;
        }
    }

    private void runGrpc(List<AviatorDiagnosticStageResult> results, AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
        try {
            var grpc = probe.probeGrpc(connectionPlan.originalUrl(), timeoutSeconds);
            var evidence = JsonHelper.getObjectMapper().createObjectNode();
            evidence.put("grpcResponseReceived", grpc.responseReceived());
            evidence.put("grpcStatusCode", grpc.statusCode());
            if (grpc.description() != null) {
                evidence.put("grpcDescription", grpc.description());
            }
            if (grpc.failureCategory() != null) {
                evidence.put("failureCategory", grpc.failureCategory());
            }
            evidence.put("httpResponseReceived", grpc.httpResponseReceived());
            if (grpc.httpStatusCode() != null) {
                evidence.put("httpStatusCode", grpc.httpStatusCode());
            }
            if (grpc.httpContentType() != null) {
                evidence.put("httpContentType", grpc.httpContentType());
            }
            if (grpc.responseReceived()) {
                results.add(pass(5, AviatorDiagnosticStage.GRPC, "Aviator gRPC responded",
                    "No action required", evidence));
            } else if (grpc.httpResponseReceived()) {
                evidence.put("pattern", "http-response-not-grpc");
                results.add(fail(5, AviatorDiagnosticStage.GRPC, "Received an HTTP page instead of gRPC",
                    "A VPN, proxy, or gateway returned a block, login, or error page; allow direct gRPC/HTTP2 to aviator-server/aviator-grpc-server", evidence));
            } else {
                evidence.put("pattern", "tls-established-grpc-no-response");
                results.add(fail(5, AviatorDiagnosticStage.GRPC, "No gRPC response received",
                    "Allow HTTP/2 gRPC traffic through the proxy, VPN, gateway, or load balancer to aviator-server/aviator-grpc-server", evidence));
            }
        } catch (Exception e) {
            var evidence = errorEvidence(e);
            evidence.put("grpcResponseReceived", false);
            evidence.put("pattern", "tls-established-grpc-no-response");
            results.add(fail(5, AviatorDiagnosticStage.GRPC, "gRPC probe failed",
                "Allow HTTP/2 gRPC traffic through the proxy, VPN, gateway, or load balancer to aviator-server/aviator-grpc-server", evidence));
        }
    }

    private ObjectNode endpointEvidence(AviatorConnectionPlan connectionPlan, String sourceType) {
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        evidence.put("sourceType", sourceType);
        evidence.put("targetHost", connectionPlan.target().host());
        evidence.put("targetPort", connectionPlan.effectivePort());
        evidence.put("normalizedUrl", connectionPlan.normalizedUrl());
        connectionPlan.proxyDescriptor().ifPresent(proxy -> {
            evidence.put("proxyHost", proxy.getProxyHost());
            evidence.put("proxyPort", proxy.getProxyPort());
            evidence.put("proxyAuthConfigured", proxy.getProxyUser() != null);
        });
        return evidence;
    }

    private static boolean isHttp2Protocol(String applicationProtocol) {
        var expected = "h2".getBytes(StandardCharsets.UTF_8);
        var actual = (applicationProtocol == null ? "" : applicationProtocol).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static void addAddresses(ObjectNode evidence, String fieldName, InetAddress[] addresses) {
        var array = evidence.putArray(fieldName);
        Arrays.stream(addresses).map(InetAddress::getHostAddress).forEach(array::add);
    }

    private static void addSkipped(List<AviatorDiagnosticStageResult> results, int order, AviatorDiagnosticStage stage, String summary) {
        results.add(warn(order, stage, summary, "Resolve the previous failed required stage first", true,
            JsonHelper.getObjectMapper().createObjectNode()));
    }

    public static AviatorDiagnosticStageResult pass(int order, AviatorDiagnosticStage stage, String summary,
            String guidance, ObjectNode evidence) {
        return AviatorDiagnosticStageResult.of(order, stage, AviatorDiagnosticStatus.PASS, true, summary, guidance, evidence);
    }

    public static AviatorDiagnosticStageResult fail(int order, AviatorDiagnosticStage stage, String summary,
            String guidance, ObjectNode evidence) {
        return AviatorDiagnosticStageResult.of(order, stage, AviatorDiagnosticStatus.FAIL, true, summary, guidance, evidence);
    }

    public static AviatorDiagnosticStageResult warn(int order, AviatorDiagnosticStage stage, String summary,
            String guidance, boolean required, ObjectNode evidence) {
        return AviatorDiagnosticStageResult.of(order, stage, AviatorDiagnosticStatus.WARN, required, summary, guidance, evidence);
    }

    public static AviatorDiagnosticStageResult optionalFail(int order, AviatorDiagnosticStage stage, String summary,
            String guidance, ObjectNode evidence) {
        return AviatorDiagnosticStageResult.of(order, stage, AviatorDiagnosticStatus.FAIL, false, summary, guidance, evidence);
    }

    public static ObjectNode errorEvidence(Exception e) {
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        evidence.put("exceptionType", e.getClass().getName());
        evidence.put("exceptionMessage", e.getMessage());
        if (e instanceof AviatorSimpleException && e.getCause() != null) {
            evidence.put("causeType", e.getCause().getClass().getName());
            evidence.put("causeMessage", e.getCause().getMessage());
        }
        return evidence;
    }
}