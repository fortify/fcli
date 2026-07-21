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

/**
 * Pure classification of gRPC probe outcomes for the GRPC diagnostic stage.
 * Never claims "TLS established" when the failure is a TLS/trust problem on the gRPC path.
 */
public final class AviatorGrpcStageClassification {
    private static final String TLS_GUIDANCE =
        "Check certificate trust, SNI, TLS inspection, and that the proxy does not break TLS to aviator-grpc-server";
    private static final String NO_RESPONSE_GUIDANCE =
        "Allow HTTP/2 gRPC traffic through the proxy, VPN, gateway, or load balancer to aviator-server/aviator-grpc-server";

    private AviatorGrpcStageClassification() {}

    public static Result classify(AviatorGrpcReachabilityResult grpc) {
        if (grpc.responseReceived()) {
            return Result.pass("Aviator gRPC responded", "No action required", null);
        }
        if (grpc.httpResponseReceived() || grpc.failureCategory() == AviatorGrpcFailureCategory.NON_GRPC_HTTP) {
            return Result.fail("Received an HTTP page instead of gRPC",
                "A VPN, proxy, or gateway returned a block, login, or error page; allow direct gRPC/HTTP2 to aviator-server/aviator-grpc-server",
                AviatorGrpcPattern.HTTP_RESPONSE_NOT_GRPC);
        }
        if (grpc.failureCategory() == AviatorGrpcFailureCategory.TLS) {
            return Result.fail("gRPC TLS handshake failed", TLS_GUIDANCE, AviatorGrpcPattern.GRPC_TLS_FAILED);
        }
        return Result.fail("No gRPC response received", NO_RESPONSE_GUIDANCE,
            AviatorGrpcPattern.TLS_ESTABLISHED_GRPC_NO_RESPONSE);
    }

    public static Result classifyException(Exception e) {
        if (AviatorTlsFailureDetector.isTlsFailure(e)) {
            return Result.fail("gRPC TLS handshake failed", TLS_GUIDANCE, AviatorGrpcPattern.GRPC_TLS_FAILED);
        }
        return Result.fail("gRPC probe failed", NO_RESPONSE_GUIDANCE, AviatorGrpcPattern.GRPC_PROBE_ERROR);
    }

    public record Result(boolean pass, String summary, String guidance, AviatorGrpcPattern pattern) {
        static Result pass(String summary, String guidance, AviatorGrpcPattern pattern) {
            return new Result(true, summary, guidance, pattern);
        }

        static Result fail(String summary, String guidance, AviatorGrpcPattern pattern) {
            return new Result(false, summary, guidance, pattern);
        }
    }
}
