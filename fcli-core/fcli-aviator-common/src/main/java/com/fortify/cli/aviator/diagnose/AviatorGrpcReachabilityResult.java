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

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Sealed gRPC probe outcome: taxonomy, stage summary/guidance, and wire {@code pattern} live here
 * (no separate failure-category + classification re-label layer).
 */
public sealed interface AviatorGrpcReachabilityResult {

    String TLS_GUIDANCE =
        "Check certificate trust, SNI, TLS inspection, and that the proxy does not break TLS to aviator-grpc-server";
    String NO_RESPONSE_GUIDANCE =
        "Allow HTTP/2 gRPC traffic through the proxy, VPN, gateway, or load balancer to aviator-grpc-server";

    String statusCode();

    String description();

    boolean stagePass();

    String stageSummary();

    String stageGuidance();

    /** Public evidence pattern wire id, or {@code null} when the stage passes. */
    AviatorGrpcPattern pattern();

    void putEvidence(ObjectNode evidence);

    /** Application-level or OK status codes still count as reachability success. */
    static AviatorGrpcReachabilityResult responseReceived(String statusCode, String description) {
        return new ResponseReceived(statusCode, description);
    }

    static AviatorGrpcReachabilityResult nonGrpcHttp(String statusCode, String httpStatus, String contentType,
            String description) {
        return new NonGrpcHttp(statusCode, httpStatus, contentType, description);
    }

    static AviatorGrpcReachabilityResult tlsFailed(String statusCode, String description) {
        return new TlsFailed(statusCode, description);
    }

    static AviatorGrpcReachabilityResult noResponse(String statusCode, String description) {
        return new NoResponse(statusCode, description);
    }

    /**
     * Exception thrown outside a classified gRPC status. Always preserves exception/cause evidence;
     * TLS vs probe-error pattern is chosen from the exception chain.
     */
    static AviatorGrpcReachabilityResult probeError(Exception e) {
        return new ProbeError(e);
    }

    record ResponseReceived(String statusCode, String description) implements AviatorGrpcReachabilityResult {
        @Override
        public boolean stagePass() {
            return true;
        }

        @Override
        public String stageSummary() {
            return "Aviator gRPC responded";
        }

        @Override
        public String stageGuidance() {
            return "No action required";
        }

        @Override
        public AviatorGrpcPattern pattern() {
            return null;
        }

        @Override
        public void putEvidence(ObjectNode evidence) {
            evidence.put("grpcResponseReceived", true);
            evidence.put("grpcStatusCode", statusCode);
            putDescription(evidence, description);
            evidence.put("httpResponseReceived", false);
        }
    }

    record NonGrpcHttp(String statusCode, String httpStatusCode, String httpContentType, String description)
            implements AviatorGrpcReachabilityResult {
        @Override
        public boolean stagePass() {
            return false;
        }

        @Override
        public String stageSummary() {
            return "Received an HTTP page instead of gRPC";
        }

        @Override
        public String stageGuidance() {
            return "A VPN, proxy, or gateway returned a block, login, or error page; allow direct gRPC/HTTP2 to aviator-grpc-server";
        }

        @Override
        public AviatorGrpcPattern pattern() {
            return AviatorGrpcPattern.HTTP_RESPONSE_NOT_GRPC;
        }

        @Override
        public void putEvidence(ObjectNode evidence) {
            evidence.put("grpcResponseReceived", false);
            evidence.put("grpcStatusCode", statusCode);
            putDescription(evidence, description);
            evidence.put("httpResponseReceived", true);
            if (httpStatusCode != null) {
                evidence.put("httpStatusCode", httpStatusCode);
            }
            if (httpContentType != null) {
                evidence.put("httpContentType", httpContentType);
            }
        }
    }

    record TlsFailed(String statusCode, String description) implements AviatorGrpcReachabilityResult {
        @Override
        public boolean stagePass() {
            return false;
        }

        @Override
        public String stageSummary() {
            return "gRPC TLS handshake failed";
        }

        @Override
        public String stageGuidance() {
            return TLS_GUIDANCE;
        }

        @Override
        public AviatorGrpcPattern pattern() {
            return AviatorGrpcPattern.GRPC_TLS_FAILED;
        }

        @Override
        public void putEvidence(ObjectNode evidence) {
            putNoResponseEvidence(evidence, statusCode, description);
        }
    }

    record NoResponse(String statusCode, String description) implements AviatorGrpcReachabilityResult {
        @Override
        public boolean stagePass() {
            return false;
        }

        @Override
        public String stageSummary() {
            return "No gRPC response received";
        }

        @Override
        public String stageGuidance() {
            return NO_RESPONSE_GUIDANCE;
        }

        @Override
        public AviatorGrpcPattern pattern() {
            return AviatorGrpcPattern.GRPC_NO_RESPONSE;
        }

        @Override
        public void putEvidence(ObjectNode evidence) {
            putNoResponseEvidence(evidence, statusCode, description);
        }
    }

    /**
     * Exception path: always includes exception/cause evidence keys; pattern is TLS or probe-error.
     */
    record ProbeError(Exception error) implements AviatorGrpcReachabilityResult {
        public ProbeError {
            if (error == null) {
                throw new NullPointerException("error");
            }
        }

        private boolean tlsFailure() {
            return AviatorTlsFailureDetector.isTlsFailure(error);
        }

        @Override
        public String statusCode() {
            return "EXCEPTION";
        }

        @Override
        public String description() {
            return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
        }

        @Override
        public boolean stagePass() {
            return false;
        }

        @Override
        public String stageSummary() {
            return tlsFailure() ? "gRPC TLS handshake failed" : "gRPC probe failed";
        }

        @Override
        public String stageGuidance() {
            return tlsFailure() ? TLS_GUIDANCE : NO_RESPONSE_GUIDANCE;
        }

        @Override
        public AviatorGrpcPattern pattern() {
            return tlsFailure() ? AviatorGrpcPattern.GRPC_TLS_FAILED : AviatorGrpcPattern.GRPC_PROBE_ERROR;
        }

        @Override
        public void putEvidence(ObjectNode evidence) {
            AviatorDiagnosticEvidence.merge(evidence, AviatorDiagnosticEvidence.errorEvidence(error));
            evidence.put("grpcResponseReceived", false);
            evidence.put("grpcStatusCode", statusCode());
            putDescription(evidence, description());
            evidence.put("httpResponseReceived", false);
        }
    }

    private static void putNoResponseEvidence(ObjectNode evidence, String statusCode, String description) {
        evidence.put("grpcResponseReceived", false);
        evidence.put("grpcStatusCode", statusCode);
        putDescription(evidence, description);
        evidence.put("httpResponseReceived", false);
    }

    private static void putDescription(ObjectNode evidence, String description) {
        if (description != null) {
            evidence.put("grpcDescription", description);
        }
    }
}
