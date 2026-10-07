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

import com.fortify.cli.common.json.JsonHelper;

/**
 * Focused on non-obvious sealed-result behavior: pattern choice + evidence shape on
 * exception paths. Pipeline wiring of patterns is covered by {@link AviatorConnectionDiagnosticsTest}.
 */
class AviatorGrpcReachabilityResultTest {
    @Test
    void putEvidenceShapesAndPatternsForStatusVariants() {
        var response = JsonHelper.getObjectMapper().createObjectNode();
        var ok = AviatorGrpcReachabilityResult.responseReceived("OK", "ok");
        assertTrue(ok.stagePass());
        assertNull(ok.pattern());
        ok.putEvidence(response);
        assertTrue(response.path("grpcResponseReceived").asBoolean());
        assertEquals("OK", response.path("grpcStatusCode").asText());
        assertFalse(response.path("httpResponseReceived").asBoolean());

        var http = JsonHelper.getObjectMapper().createObjectNode();
        var nonGrpc = AviatorGrpcReachabilityResult.nonGrpcHttp("UNAVAILABLE", "503", "text/html", "page");
        assertEquals(AviatorGrpcPattern.HTTP_RESPONSE_NOT_GRPC, nonGrpc.pattern());
        nonGrpc.putEvidence(http);
        assertFalse(http.path("grpcResponseReceived").asBoolean());
        assertTrue(http.path("httpResponseReceived").asBoolean());
        assertEquals("503", http.path("httpStatusCode").asText());

        var tls = JsonHelper.getObjectMapper().createObjectNode();
        var tlsFailed = AviatorGrpcReachabilityResult.tlsFailed("UNAVAILABLE", "ssl");
        assertEquals(AviatorGrpcPattern.GRPC_TLS_FAILED, tlsFailed.pattern());
        tlsFailed.putEvidence(tls);
        assertFalse(tls.has("exceptionType"));

        var noResponse = JsonHelper.getObjectMapper().createObjectNode();
        var none = AviatorGrpcReachabilityResult.noResponse("DEADLINE_EXCEEDED", "deadline");
        assertEquals(AviatorGrpcPattern.GRPC_NO_RESPONSE, none.pattern());
        none.putEvidence(noResponse);
        assertEquals("DEADLINE_EXCEEDED", noResponse.path("grpcStatusCode").asText());
    }

    @Test
    void probeErrorTlsKeepsExceptionCauseEvidence() {
        var wrapped = new Exception("io exception", new SSLHandshakeException("PKIX path building failed"));
        var result = AviatorGrpcReachabilityResult.probeError(wrapped);

        assertEquals(AviatorGrpcPattern.GRPC_TLS_FAILED, result.pattern());
        assertEquals("gRPC TLS handshake failed", result.stageSummary());

        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        result.putEvidence(evidence);
        assertEquals("java.lang.Exception", evidence.path("exceptionType").asText());
        assertEquals(SSLHandshakeException.class.getName(), evidence.path("causeType").asText());
        assertFalse(evidence.path("grpcResponseReceived").asBoolean());
        assertEquals("EXCEPTION", evidence.path("grpcStatusCode").asText());
    }

    @Test
    void probeErrorGenericKeepsExceptionEvidence() {
        var result = AviatorGrpcReachabilityResult.probeError(new RuntimeException("boom"));

        assertEquals(AviatorGrpcPattern.GRPC_PROBE_ERROR, result.pattern());
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        result.putEvidence(evidence);
        assertEquals("java.lang.RuntimeException", evidence.path("exceptionType").asText());
        assertEquals("boom", evidence.path("exceptionMessage").asText());
    }
}
