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
package com.fortify.cli.aviator.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ConnectException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;

import com.fortify.aviator.application.ApplicationServiceGrpc;
import com.fortify.aviator.grpc.AuditorResponse;
import com.fortify.cli.aviator._common.exception.AviatorConnectivityException;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResponse.AuditSkipReason;
import com.fortify.cli.aviator.audit.model.File;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.common.exception.FcliExceptionHelper;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

class GrpcUtilTest {

    @Test
    void shouldPreserveResolvedProgrammingLanguages() {
        File file = new File();
        file.setName("db/t1.sql");

        UserPrompt userPrompt = UserPrompt.builder()
                .files(List.of(file))
                .programmingLanguages(Set.of("PLSQL"))
                .fileExtension("sql")
                .language("PLSQL")
                .category("SQL Injection")
                .build();

        com.fortify.aviator.grpc.AuditRequest auditRequest =
                GrpcUtil.convertToAuditRequest(userPrompt, "stream-1", "request-1");

        assertEquals(List.of("PLSQL"), auditRequest.getProgrammingLanguagesList());
        assertEquals("PLSQL", auditRequest.getLanguage());
    }

    @Test
    void shouldClassifySkippedServerResponseWithoutInspectingMessage() {
        AuditorResponse response = AuditorResponse.newBuilder()
                .setStatus("SKIPPED")
                .setStatusMessage("example could not be read from the FPR")
                .build();

        AuditResponse auditResponse = GrpcUtil.convertToAuditResponse(response);

        assertEquals(AuditSkipReason.SKIPPED_BY_AVIATOR, auditResponse.getAuditSkipReason());
    }

    @Test
    void waitForReadyPolicyKeepsCurrentUnavailableMapping() {
        var e = assertThrows(AviatorTechnicalException.class,
            () -> execute(Status.UNAVAILABLE.withCause(new ConnectException("Connection refused")), AviatorRequestPolicy.WAIT_FOR_READY));

        assertTrue(e.getMessage().startsWith("A technical error occurred on the server while performing the test operation"));
    }

    @Test
    void failFastPolicyClassifiesUnavailable() {
        var e = assertThrows(AviatorConnectivityException.class,
            () -> execute(Status.UNAVAILABLE.withCause(new ConnectException("Connection refused")), AviatorRequestPolicy.FAIL_FAST));

        assertEquals("Aviator server 127.0.0.1:1 refused the connection; check the Aviator URL", e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void failFastPolicyKeepsCertificateCauseForPkixGuidance() {
        var e = assertThrows(AviatorTechnicalException.class,
            () -> execute(Status.UNAVAILABLE.withCause(new SSLHandshakeException("PKIX path building failed")
                .initCause(new CertificateException("untrusted"))), AviatorRequestPolicy.FAIL_FAST));

        assertTrue(FcliExceptionHelper.formatException(e).contains("PKIX SSL certificate validation failed"));
    }

    @Test
    void failFastPolicyKeepsUnimplementedContractForOlderServerLoginFallback() {
        var e = assertThrows(AviatorTechnicalException.class,
            () -> execute(Status.UNIMPLEMENTED, AviatorRequestPolicy.FAIL_FAST));

        var cause = assertInstanceOf(StatusRuntimeException.class, e.getCause());
        assertEquals(Status.Code.UNIMPLEMENTED, cause.getStatus().getCode());
    }

    @Test
    void failFastPolicyKeepsApplicationStatusMapping() {
        var e = assertThrows(AviatorSimpleException.class,
            () -> execute(Status.NOT_FOUND.withDescription("app"), AviatorRequestPolicy.FAIL_FAST));

        assertFalse(e instanceof AviatorConnectivityException);
        assertTrue(e.getMessage().contains("not found"));
    }

    private static void execute(Status status, AviatorRequestPolicy policy) {
        var channel = NettyChannelBuilder.forAddress("127.0.0.1", 1).usePlaintext().build();
        try {
            var context = new AviatorConnectionContext(new AviatorEndpoint("127.0.0.1", 1), null);
            GrpcUtil.executeGrpcCall(policy.configureNewStub(ApplicationServiceGrpc.newBlockingStub(channel), context),
                (stub, request) -> { throw status.asRuntimeException(); },
                "request", "test");
        } finally {
            channel.shutdownNow();
        }
    }
}