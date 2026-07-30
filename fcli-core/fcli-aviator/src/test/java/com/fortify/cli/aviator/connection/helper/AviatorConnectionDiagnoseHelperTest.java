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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper.AviatorUserTokenValidationResult;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseHelper.AdminValidator;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseHelper.TokenValidator;
import com.fortify.cli.aviator.diagnose.AviatorConnectionDiagnostics;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticReport;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStageResult;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStatus;
import com.fortify.cli.aviator.diagnose.AviatorGrpcReachabilityResult;
import com.fortify.cli.aviator.diagnose.IAviatorDiagnosticProbe;
import com.fortify.cli.aviator.diagnose.support.ConfigurableDiagnosticProbe;
import com.fortify.cli.aviator.diagnose.support.OfflineConnectionPlan;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.grpc.token.TokenValidationResponse;

/**
 * Product-layer credential policy: omit / skip / optional pass-fail + sourceType matrix.
 * Transport skip chains live in {@code AviatorConnectionDiagnosticsTest}.
 */
class AviatorConnectionDiagnoseHelperTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("sourceTypes")
    void endpointSourceType(String label, AviatorConnectionDiagnoseSource source, String expectedSourceType) {
        var result = helperWithGrpc(AviatorGrpcReachabilityResult.responseReceived("OK", "ok"))
            .diagnose(source, 5);
        assertEquals(expectedSourceType, result.stages().get(0).evidence().path("sourceType").asText());
    }

    static Stream<Arguments> sourceTypes() {
        return Stream.of(
            Arguments.of("url", AviatorConnectionDiagnoseSource.fromUrl("https://aviator.invalid"), "url"),
            Arguments.of("url-token",
                AviatorConnectionDiagnoseSource.fromUrlAndToken("https://aviator.invalid", "tok"), "url-token"),
            Arguments.of("user-session",
                AviatorConnectionDiagnoseSource.fromUserSession(session("session-tok")), "user-session"),
            Arguments.of("admin-config",
                AviatorConnectionDiagnoseSource.fromAdminConfig(admin()), "admin-config"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("credentialSources")
    void credentialSkippedWhenGrpcDidNotRespond(String label, AviatorConnectionDiagnoseSource source,
            String expectedStage) {
        var result = helperWithGrpc(AviatorGrpcReachabilityResult.noResponse("DEADLINE_EXCEEDED", "deadline"))
            .diagnose(source, 5);
        var cred = lastStage(result.stages());
        assertEquals(expectedStage, cred.stage());
        assertEquals(AviatorDiagnosticStatus.WARN, cred.status());
        assertFalse(cred.required());
        assertTrue(cred.summary().toLowerCase().contains("skipped"));
    }

    static Stream<Arguments> credentialSources() {
        return Stream.of(
            Arguments.of("url-token",
                AviatorConnectionDiagnoseSource.fromUrlAndToken("https://aviator.invalid", "tok"),
                AviatorConnectionDiagnoseHelper.STAGE_TOKEN),
            Arguments.of("user-session",
                AviatorConnectionDiagnoseSource.fromUserSession(session("session-tok")),
                AviatorConnectionDiagnoseHelper.STAGE_TOKEN),
            Arguments.of("admin-config",
                AviatorConnectionDiagnoseSource.fromAdminConfig(admin()),
                AviatorConnectionDiagnoseHelper.STAGE_ADMIN));
    }

    @Test
    void bareUrlOmitsCredentialStage() {
        var result = helperWithGrpc(AviatorGrpcReachabilityResult.responseReceived("OK", "ok"))
            .diagnose(AviatorConnectionDiagnoseSource.fromUrl("https://aviator.invalid"), 5);

        assertTrue(result.stages().stream().noneMatch(s ->
            AviatorConnectionDiagnoseHelper.STAGE_TOKEN.equals(s.stage())
                || AviatorConnectionDiagnoseHelper.STAGE_ADMIN.equals(s.stage())));
        assertFalse(result.requiredFailure());
    }

    @Test
    void tokenPassAndOptionalFailDoNotForceRequiredFailure() {
        TokenValidator tokenOk = (url, token) ->
            new AviatorUserTokenValidationResult("tenant", TokenValidationResponse.newBuilder().setValid(true).build());
        var pass = helperWithGrpcAndValidators(
            AviatorGrpcReachabilityResult.responseReceived("OK", "ok"), tokenOk, d -> {})
            .diagnose(AviatorConnectionDiagnoseSource.fromUrlAndToken("https://aviator.invalid", "tok"), 5);
        assertEquals(AviatorDiagnosticStatus.PASS, lastStage(pass.stages()).status());
        assertFalse(lastStage(pass.stages()).required());
        assertFalse(pass.requiredFailure());

        TokenValidator tokenBad = (url, token) ->
            new AviatorUserTokenValidationResult("tenant",
                TokenValidationResponse.newBuilder().setValid(false).setErrorMessage("expired").build());
        var fail = helperWithGrpcAndValidators(
            AviatorGrpcReachabilityResult.responseReceived("OK", "ok"), tokenBad, d -> {})
            .diagnose(AviatorConnectionDiagnoseSource.fromUrlAndToken("https://aviator.invalid", "tok"), 5);
        assertEquals(AviatorDiagnosticStatus.FAIL, lastStage(fail.stages()).status());
        assertFalse(lastStage(fail.stages()).required());
        assertFalse(fail.requiredFailure());
        assertEquals("expired", lastStage(fail.stages()).evidence().path("tokenValidationMessage").asText());
    }

    @Test
    void tokenValidatorExceptionIsOptionalFailWithExceptionEvidence() {
        TokenValidator tokenBad = (url, token) -> {
            throw new IllegalStateException("validator error");
        };
        var result = helperWithGrpcAndValidators(
            AviatorGrpcReachabilityResult.responseReceived("OK", "ok"), tokenBad, d -> {})
            .diagnose(AviatorConnectionDiagnoseSource.fromUrlAndToken("https://aviator.invalid", "tok"), 5);

        var cred = lastStage(result.stages());
        assertEquals(AviatorDiagnosticStatus.FAIL, cred.status());
        assertFalse(cred.required());
        assertFalse(result.requiredFailure());
        assertEquals("java.lang.IllegalStateException", cred.evidence().path("exceptionType").asText());
    }

    @Test
    void tokenValidatorBugExceptionIsNotSwallowed() {
        TokenValidator tokenBug = (url, token) -> {
            throw new FcliBugException("internal invariant broken");
        };
        assertThrows(FcliBugException.class, () -> helperWithGrpcAndValidators(
            AviatorGrpcReachabilityResult.responseReceived("OK", "ok"), tokenBug, d -> {})
            .diagnose(AviatorConnectionDiagnoseSource.fromUrlAndToken("https://aviator.invalid", "tok"), 5));
    }

    @Test
    void adminValidatorBugExceptionIsNotSwallowed() {
        AdminValidator adminBug = d -> {
            throw new FcliBugException("admin path bug");
        };
        assertThrows(FcliBugException.class, () -> helperWithGrpcAndValidators(
            AviatorGrpcReachabilityResult.responseReceived("OK", "ok"),
            (u, t) -> {
                throw new IllegalStateException("token path should not run");
            },
            adminBug).diagnose(AviatorConnectionDiagnoseSource.fromAdminConfig(admin()), 5));
    }

    @Test
    void adminPassUsesAdminStageAndValidator() {
        var adminCalled = new AtomicBoolean();
        AdminValidator adminOk = d -> adminCalled.set(true);
        var result = helperWithGrpcAndValidators(
            AviatorGrpcReachabilityResult.responseReceived("OK", "ok"),
            (u, t) -> {
                throw new IllegalStateException("token path should not run");
            },
            adminOk).diagnose(AviatorConnectionDiagnoseSource.fromAdminConfig(admin()), 5);

        assertTrue(adminCalled.get());
        assertEquals(AviatorConnectionDiagnoseHelper.STAGE_ADMIN, lastStage(result.stages()).stage());
        assertEquals(AviatorDiagnosticStatus.PASS, lastStage(result.stages()).status());
        assertFalse(result.requiredFailure());
    }

    @Test
    void userSessionWithMissingTokenOptionalFailsWithoutNpe() {
        var result = helperWithGrpc(AviatorGrpcReachabilityResult.responseReceived("OK", "ok"))
            .diagnose(AviatorConnectionDiagnoseSource.fromUserSession(session(null)), 5);

        var cred = lastStage(result.stages());
        assertEquals(AviatorConnectionDiagnoseHelper.STAGE_TOKEN, cred.stage());
        assertEquals(AviatorDiagnosticStatus.FAIL, cred.status());
        assertFalse(cred.required());
        assertFalse(result.requiredFailure());
        assertTrue(cred.summary().toLowerCase().contains("missing"));
    }

    private static AviatorUserSessionDescriptor session(String token) {
        return AviatorUserSessionDescriptor.builder()
            .aviatorUrl("https://aviator.invalid")
            .aviatorToken(token)
            .build();
    }

    private static AviatorAdminConfigDescriptor admin() {
        return AviatorAdminConfigDescriptor.builder()
            .aviatorUrl("https://aviator.invalid")
            .tenant("demo")
            .build();
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
            new OfflineDiagnostics(new ConfigurableDiagnosticProbe(grpc)), tokenValidator, adminValidator);
    }

    private static final class OfflineDiagnostics extends AviatorConnectionDiagnostics {
        OfflineDiagnostics(IAviatorDiagnosticProbe probe) {
            super(probe);
        }

        @Override
        public AviatorDiagnosticReport diagnose(String url, int timeoutSeconds, String sourceType) {
            return diagnose(OfflineConnectionPlan.fixedUrl(url), timeoutSeconds, sourceType);
        }
    }
}
