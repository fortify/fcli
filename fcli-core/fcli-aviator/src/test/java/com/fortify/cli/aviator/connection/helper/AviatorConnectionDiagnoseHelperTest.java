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
package com.fortify.cli.aviator.connection.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper.AviatorUserTokenValidationResult;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseHelper.AdminValidator;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseHelper.TokenValidator;
import com.fortify.cli.aviator.diagnose.AviatorConnectionDiagnostics;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStage;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStageResult;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStatus;
import com.fortify.cli.aviator.diagnose.AviatorGrpcFailureCategory;
import com.fortify.cli.aviator.diagnose.AviatorGrpcReachabilityResult;
import com.fortify.cli.aviator.diagnose.AviatorTunnelResult;
import com.fortify.cli.aviator.diagnose.IAviatorDiagnosticProbe;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.ParsedTarget;
import com.fortify.grpc.token.TokenValidationResponse;

class AviatorConnectionDiagnoseHelperTest {

    @Test
    void bareUrlOmitsCredentialStage() {
        var helper = helperWithGrpc(AviatorGrpcReachabilityResult.ok("OK", "ok"));
        var result = helper.diagnose(AviatorConnectionDiagnoseSource.fromUrl("https://aviator.example.com"), 5);

        assertTrue(result.stages().stream().noneMatch(s ->
            s.stage() == AviatorDiagnosticStage.TOKEN || s.stage() == AviatorDiagnosticStage.ADMIN));
        assertFalse(result.requiredFailure());
    }

    @Test
    void urlAndTokenSkipsCredentialWhenGrpcDidNotRespond() {
        var helper = helperWithGrpc(AviatorGrpcReachabilityResult.noResponse(
            "DEADLINE_EXCEEDED", AviatorGrpcFailureCategory.NO_RESPONSE, "deadline"));
        var result = helper.diagnose(
            AviatorConnectionDiagnoseSource.fromUrlAndToken("https://aviator.example.com", "tok"), 5);

        var cred = lastStage(result.stages());
        assertEquals(AviatorDiagnosticStage.TOKEN, cred.stage());
        assertEquals(AviatorDiagnosticStatus.WARN, cred.status());
        assertFalse(cred.required());
        assertTrue(cred.summary().toLowerCase().contains("skipped"));
    }

    @Test
    void userSessionSkipsAsTokenStageWhenGrpcDidNotRespond() {
        var session = AviatorUserSessionDescriptor.builder()
            .aviatorUrl("https://aviator.example.com")
            .aviatorToken("session-tok")
            .build();
        var helper = helperWithGrpc(AviatorGrpcReachabilityResult.noResponse(
            "DEADLINE_EXCEEDED", AviatorGrpcFailureCategory.NO_RESPONSE, "deadline"));
        var result = helper.diagnose(AviatorConnectionDiagnoseSource.fromUserSession(session), 5);

        var cred = lastStage(result.stages());
        assertEquals(AviatorDiagnosticStage.TOKEN, cred.stage());
        assertEquals(AviatorDiagnosticStatus.WARN, cred.status());
        assertFalse(result.stages().stream().anyMatch(s -> s.stage() == AviatorDiagnosticStage.ADMIN));
    }

    @Test
    void adminConfigSkipsAsAdminStageWhenGrpcDidNotRespond() {
        var admin = AviatorAdminConfigDescriptor.builder()
            .aviatorUrl("https://aviator.example.com")
            .tenant("demo")
            .build();
        var helper = helperWithGrpc(AviatorGrpcReachabilityResult.noResponse(
            "DEADLINE_EXCEEDED", AviatorGrpcFailureCategory.NO_RESPONSE, "deadline"));
        var result = helper.diagnose(AviatorConnectionDiagnoseSource.fromAdminConfig(admin), 5);

        var cred = lastStage(result.stages());
        assertEquals(AviatorDiagnosticStage.ADMIN, cred.stage());
        assertEquals(AviatorDiagnosticStatus.WARN, cred.status());
        assertFalse(result.stages().stream().anyMatch(s -> s.stage() == AviatorDiagnosticStage.TOKEN));
    }

    @Test
    void urlAndTokenPassWhenValidatorAccepts() {
        TokenValidator tokenOk = (url, token) ->
            new AviatorUserTokenValidationResult("tenant", TokenValidationResponse.newBuilder().setValid(true).build());
        var helper = helperWithGrpcAndValidators(
            AviatorGrpcReachabilityResult.ok("OK", "ok"), tokenOk, d -> {});
        var result = helper.diagnose(
            AviatorConnectionDiagnoseSource.fromUrlAndToken("https://aviator.example.com", "tok"), 5);

        var cred = lastStage(result.stages());
        assertEquals(AviatorDiagnosticStage.TOKEN, cred.stage());
        assertEquals(AviatorDiagnosticStatus.PASS, cred.status());
        assertFalse(cred.required());
        assertFalse(result.requiredFailure());
    }

    @Test
    void urlAndTokenOptionalFailDoesNotSetRequiredFailure() {
        TokenValidator tokenBad = (url, token) ->
            new AviatorUserTokenValidationResult("tenant",
                TokenValidationResponse.newBuilder().setValid(false).setErrorMessage("expired").build());
        var helper = helperWithGrpcAndValidators(
            AviatorGrpcReachabilityResult.ok("OK", "ok"), tokenBad, d -> {});
        var result = helper.diagnose(
            AviatorConnectionDiagnoseSource.fromUrlAndToken("https://aviator.example.com", "tok"), 5);

        var cred = lastStage(result.stages());
        assertEquals(AviatorDiagnosticStage.TOKEN, cred.stage());
        assertEquals(AviatorDiagnosticStatus.FAIL, cred.status());
        assertFalse(cred.required());
        assertFalse(result.requiredFailure());
        assertEquals("expired", cred.evidence().path("tokenValidationMessage").asText());
    }

    @Test
    void urlAndTokenOptionalFailWhenValidatorThrows() {
        TokenValidator tokenBad = (url, token) -> {
            throw new IllegalStateException("validator error");
        };
        var helper = helperWithGrpcAndValidators(
            AviatorGrpcReachabilityResult.ok("OK", "ok"), tokenBad, d -> {});
        var result = helper.diagnose(
            AviatorConnectionDiagnoseSource.fromUrlAndToken("https://aviator.example.com", "tok"), 5);

        var cred = lastStage(result.stages());
        assertEquals(AviatorDiagnosticStage.TOKEN, cred.stage());
        assertEquals(AviatorDiagnosticStatus.FAIL, cred.status());
        assertFalse(cred.required());
        assertFalse(result.requiredFailure());
        assertEquals("java.lang.IllegalStateException", cred.evidence().path("exceptionType").asText());
    }

    @Test
    void adminPassWhenValidatorAccepts() {
        var adminCalled = new AtomicBoolean();
        AdminValidator adminOk = d -> adminCalled.set(true);
        var admin = AviatorAdminConfigDescriptor.builder()
            .aviatorUrl("https://aviator.example.com")
            .tenant("demo")
            .build();
        var helper = helperWithGrpcAndValidators(
            AviatorGrpcReachabilityResult.ok("OK", "ok"),
            (u, t) -> {
                throw new IllegalStateException("token path should not run");
            },
            adminOk);
        var result = helper.diagnose(AviatorConnectionDiagnoseSource.fromAdminConfig(admin), 5);

        assertTrue(adminCalled.get());
        var cred = lastStage(result.stages());
        assertEquals(AviatorDiagnosticStage.ADMIN, cred.stage());
        assertEquals(AviatorDiagnosticStatus.PASS, cred.status());
        assertFalse(result.requiredFailure());
    }

    @Test
    void adminOptionalFailWhenValidatorThrows() {
        var admin = AviatorAdminConfigDescriptor.builder()
            .aviatorUrl("https://aviator.example.com")
            .tenant("demo")
            .build();
        var helper = helperWithGrpcAndValidators(
            AviatorGrpcReachabilityResult.ok("OK", "ok"),
            (u, t) -> {
                throw new IllegalStateException("token path should not run");
            },
            d -> {
                throw new RuntimeException("bad keys");
            });
        var result = helper.diagnose(AviatorConnectionDiagnoseSource.fromAdminConfig(admin), 5);

        var cred = lastStage(result.stages());
        assertEquals(AviatorDiagnosticStage.ADMIN, cred.stage());
        assertEquals(AviatorDiagnosticStatus.FAIL, cred.status());
        assertFalse(cred.required());
        assertFalse(result.requiredFailure());
    }

    private static AviatorDiagnosticStageResult lastStage(List<AviatorDiagnosticStageResult> stages) {
        return stages.get(stages.size() - 1);
    }

    private static AviatorConnectionDiagnoseHelper helperWithGrpc(AviatorGrpcReachabilityResult grpc) {
        return helperWithGrpcAndValidators(grpc,
            (u, t) -> {
                throw new IllegalStateException("token validator not expected");
            },
            d -> {
                throw new IllegalStateException("admin validator not expected");
            });
    }

    private static AviatorConnectionDiagnoseHelper helperWithGrpcAndValidators(
            AviatorGrpcReachabilityResult grpc,
            TokenValidator tokenValidator,
            AdminValidator adminValidator) {
        return new AviatorConnectionDiagnoseHelper(
            new OfflineDiagnostics(new FakeProbe(grpc)), tokenValidator, adminValidator);
    }

    /**
     * Uses a fixed connection plan so tests never touch ambient proxy env or real DNS policy.
     */
    private static final class OfflineDiagnostics extends AviatorConnectionDiagnostics {
        OfflineDiagnostics(IAviatorDiagnosticProbe probe) {
            super(probe);
        }

        @Override
        public List<AviatorDiagnosticStageResult> diagnose(String url, int timeoutSeconds, String sourceType) {
            var plan = new AviatorConnectionPlan(url, url,
                new ParsedTarget("aviator.example.com", null), 443, Optional.empty());
            return diagnose(plan, timeoutSeconds, sourceType);
        }
    }

    private static final class FakeProbe implements IAviatorDiagnosticProbe {
        private final AviatorGrpcReachabilityResult grpcResult;

        FakeProbe(AviatorGrpcReachabilityResult grpcResult) {
            this.grpcResult = grpcResult;
        }

        @Override
        public InetAddress[] resolve(String host) {
            try {
                return new InetAddress[] {InetAddress.getByName("127.0.0.1")};
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void connect(String host, int port, int timeoutSeconds) {}

        @Override
        public AviatorTunnelResult probeTunnel(AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
            return new AviatorTunnelResult.TlsSucceeded(false, "not-used",
                "TLSv1.3", "TLS_AES_128_GCM_SHA256", "CN=aviator.example.com", "h2");
        }

        @Override
        public AviatorGrpcReachabilityResult probeGrpc(String url, int timeoutSeconds) {
            return grpcResult;
        }
    }
}
