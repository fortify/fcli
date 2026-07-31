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

import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigHelper;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper.AviatorUserTokenValidationResult;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseSource.CredentialRequest;
import com.fortify.cli.aviator.diagnose.AviatorConnectionDiagnostics;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticEvidence;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticReport;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStageResult;
import com.fortify.cli.common.exception.FcliBugException;

import lombok.RequiredArgsConstructor;

/**
 * Orchestrates transport diagnostics then an optional credential stage.
 * <p>
 * Soft-exit policy lives in the command: credential failures are optional
 * ({@code required=false}) so they never alone force a non-zero process exit.
 * Product credential stage ids ({@code token}/{@code admin}) are not transport stages.
 */
@RequiredArgsConstructor
public class AviatorConnectionDiagnoseHelper {
    public static final String STAGE_TOKEN = "token";
    public static final String STAGE_TOKEN_DESCRIPTION = "Aviator token validation";
    public static final String STAGE_ADMIN = "admin";
    public static final String STAGE_ADMIN_DESCRIPTION = "Aviator admin credential validation";

    private final AviatorConnectionDiagnostics diagnostics;
    private final TokenValidator tokenValidator;
    private final AdminValidator adminValidator;

    public AviatorConnectionDiagnoseHelper() {
        this(new AviatorConnectionDiagnostics(),
            (url, token) -> AviatorUserSessionHelper.instance().validateToken(url, token),
            descriptor -> AviatorAdminConfigHelper.instance().validateConfig(descriptor));
    }

    public DiagnoseRunResult diagnose(AviatorConnectionDiagnoseSource source, int timeoutSeconds) {
        var report = diagnostics.diagnose(source.url(), timeoutSeconds, source.sourceTypeId());
        appendCredentialStage(report, source);
        return new DiagnoseRunResult(report.stages(), report.toArrayNode(), report.hasRequiredFailure());
    }

    private void appendCredentialStage(AviatorDiagnosticReport report, AviatorConnectionDiagnoseSource source) {
        source.credentialRequest().ifPresent(cred -> cred.accept(new CredentialRequest.Visitor() {
            @Override
            public void visitToken(CredentialRequest.Token token) {
                runCredentialStage(report, STAGE_TOKEN, STAGE_TOKEN_DESCRIPTION,
                    () -> validateUserToken(report, token.url(), token.token()));
            }

            @Override
            public void visitAdmin(CredentialRequest.Admin admin) {
                runCredentialStage(report, STAGE_ADMIN, STAGE_ADMIN_DESCRIPTION,
                    () -> validateAdminConfig(report, admin.descriptor()));
            }
        }));
    }

    /** Skip when gRPC did not respond; otherwise run {@code validate}. */
    private void runCredentialStage(AviatorDiagnosticReport report, String stage, String description,
            Runnable validate) {
        if (!report.hasGrpcStagePass()) {
            report.optionalSkipWarn(stage, description,
                "Credential check skipped",
                "Fix the gRPC connection first", AviatorDiagnosticEvidence.empty());
            return;
        }
        validate.run();
    }

    private void validateUserToken(AviatorDiagnosticReport report, String aviatorUrl, String token) {
        if (token == null || token.isBlank()) {
            report.optionalFail(STAGE_TOKEN, STAGE_TOKEN_DESCRIPTION,
                "Aviator token is missing",
                "Create or update the session with a valid user token", AviatorDiagnosticEvidence.empty());
            return;
        }
        try {
            var validationResult = tokenValidator.validate(aviatorUrl, token);
            var evidence = AviatorDiagnosticEvidence.empty();
            evidence.put("tenantNamePresent", validationResult.tenantName() != null);
            if (validationResult.response().getValid()) {
                report.optionalPass(STAGE_TOKEN, STAGE_TOKEN_DESCRIPTION,
                    "Aviator token is valid", "No action required", evidence);
                return;
            }
            var errorMessage = validationResult.response().getErrorMessage();
            if (errorMessage != null && !errorMessage.isBlank()) {
                evidence.put("tokenValidationMessage", errorMessage);
            }
            report.optionalFail(STAGE_TOKEN, STAGE_TOKEN_DESCRIPTION,
                "Aviator token is not valid",
                "Use a current token for the expected tenant", evidence);
        } catch (RuntimeException e) {
            rethrowIfBug(e);
            report.optionalFail(STAGE_TOKEN, STAGE_TOKEN_DESCRIPTION,
                "Aviator token check failed",
                "Use a current token for the expected tenant",
                AviatorDiagnosticEvidence.errorEvidence(e));
        }
    }

    private void validateAdminConfig(AviatorDiagnosticReport report, AviatorAdminConfigDescriptor configDescriptor) {
        try {
            adminValidator.validate(configDescriptor);
            var evidence = AviatorDiagnosticEvidence.empty();
            evidence.put("tenant", configDescriptor.getTenant());
            report.optionalPass(STAGE_ADMIN, STAGE_ADMIN_DESCRIPTION,
                "Aviator admin credentials are valid", "No action required", evidence);
        } catch (RuntimeException e) {
            rethrowIfBug(e);
            report.optionalFail(STAGE_ADMIN, STAGE_ADMIN_DESCRIPTION,
                "Admin credentials are not valid",
                "Check the tenant, public key, and private key", AviatorDiagnosticEvidence.errorEvidence(e));
        }
    }

    /**
     * Credential stages soft-fail expected validation problems into the report, but must not
     * swallow product defects ({@link FcliBugException} / {@code AviatorBugException}).
     */
    private static void rethrowIfBug(RuntimeException e) {
        if (e instanceof FcliBugException bug) {
            throw bug;
        }
    }

    @FunctionalInterface
    public interface TokenValidator {
        AviatorUserTokenValidationResult validate(String aviatorUrl, String token);
    }

    @FunctionalInterface
    public interface AdminValidator {
        void validate(AviatorAdminConfigDescriptor configDescriptor);
    }

    public record DiagnoseRunResult(List<AviatorDiagnosticStageResult> stages, ArrayNode json, boolean requiredFailure) {}
}
