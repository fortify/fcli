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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;

class AviatorGrpcStageClassificationTest {
    @Test
    void shouldPassWhenResponseReceived() {
        var result = AviatorGrpcStageClassification.classify(
            AviatorGrpcReachabilityResult.ok("UNAUTHENTICATED", "token required"));

        assertTrue(result.pass());
        assertNull(result.pattern());
    }

    @Test
    void shouldMapNonGrpcHttpResponse() {
        var result = AviatorGrpcStageClassification.classify(
            AviatorGrpcReachabilityResult.nonGrpcHttp("UNAVAILABLE", "503", "text/html", "html"));

        assertFalse(result.pass());
        assertEquals(AviatorGrpcPattern.HTTP_RESPONSE_NOT_GRPC, result.pattern());
    }

    @Test
    void shouldMapTlsFailureCategoryWithoutClaimingTlsEstablished() {
        var result = AviatorGrpcStageClassification.classify(
            AviatorGrpcReachabilityResult.noResponse("UNAVAILABLE", AviatorGrpcFailureCategory.TLS, "ssl"));

        assertEquals(AviatorGrpcPattern.GRPC_TLS_FAILED, result.pattern());
    }

    @Test
    void shouldMapNoResponseAsTlsEstablishedPattern() {
        var result = AviatorGrpcStageClassification.classify(
            AviatorGrpcReachabilityResult.noResponse("DEADLINE_EXCEEDED", AviatorGrpcFailureCategory.NO_RESPONSE, "deadline"));

        assertEquals(AviatorGrpcPattern.TLS_ESTABLISHED_GRPC_NO_RESPONSE, result.pattern());
    }

    @Test
    void shouldDetectTlsFailureFromCauseChainWhenDescriptionIsGeneric() {
        var wrapped = new Exception("io exception", new SSLHandshakeException("PKIX path building failed"));

        assertTrue(AviatorTlsFailureDetector.isTlsFailure(wrapped, "io exception"));
        assertEquals(AviatorGrpcPattern.GRPC_TLS_FAILED,
            AviatorGrpcStageClassification.classifyException(wrapped).pattern());
    }

    @Test
    void shouldMapGenericExceptionToProbeError() {
        assertEquals(AviatorGrpcPattern.GRPC_PROBE_ERROR,
            AviatorGrpcStageClassification.classifyException(new RuntimeException("boom")).pattern());
    }

    @Test
    void shouldNotTreatPlainTimeoutAsTls() {
        assertFalse(AviatorTlsFailureDetector.isTlsFailure(new RuntimeException("deadline exceeded"), "deadline exceeded"));
    }
}
