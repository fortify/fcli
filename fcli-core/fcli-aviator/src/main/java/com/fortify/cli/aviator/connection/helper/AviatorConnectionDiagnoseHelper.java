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
import java.util.function.IntConsumer;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigHelper;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper.AviatorUserTokenValidationResult;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseSource.AdminConfig;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseSource.UrlAndToken;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseSource.UrlOnly;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseSource.UserSession;
import com.fortify.cli.aviator.diagnose.AviatorConnectionDiagnostics;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStage;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStageResult;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStatus;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.json.JsonHelper;

import lombok.RequiredArgsConstructor;

/**
 * Orchestrates transport diagnostics then an optional credential stage.
 * <p>
 * Soft-exit policy lives in the command: credential failures are optional
 * ({@code required=false}) so they never alone force a non-zero process exit.
 */
@RequiredArgsConstructor
public class AviatorConnectionDiagnoseHelper {
    private final AviatorConnectionDiagnostics diagnostics;
    private final TokenValidator tokenValidator;
    private final AdminValidator adminValidator;

    public AviatorConnectionDiagnoseHelper() {
        this(new AviatorConnectionDiagnostics(),
            (url, token) -> AviatorUserSessionHelper.instance().validateToken(url, token),
            descriptor -> AviatorAdminConfigHelper.instance().validateConfig(descriptor));
    }

    public DiagnoseRunResult diagnose(AviatorConnectionDiagnoseSource source, int timeoutSeconds) {
        var results = diagnostics.diagnose(source.url(), timeoutSeconds, source.type());
        appendCredentialStage(results, source);
        return new DiagnoseRunResult(results, diagnostics.toArrayNode(results), diagnostics.hasRequiredFailure(results));
    }

    private void appendCredentialStage(List<AviatorDiagnosticStageResult> results, AviatorConnectionDiagnoseSource source) {
        if (source instanceof UrlOnly) {
            // Credentials were not requested: omit the stage entirely.
            return;
        }
        if (source instanceof UrlAndToken urlAndToken) {
            runCredentialStage(results, AviatorDiagnosticStage.TOKEN,
                order -> validateUserToken(results, order, urlAndToken.url(), urlAndToken.token()));
            return;
        }
        if (source instanceof UserSession userSession) {
            runCredentialStage(results, AviatorDiagnosticStage.TOKEN,
                order -> validateUserToken(results, order, userSession.url(),
                    userSession.descriptor().getAviatorToken()));
            return;
        }
        if (source instanceof AdminConfig adminConfig) {
            runCredentialStage(results, AviatorDiagnosticStage.ADMIN,
                order -> validateAdminConfig(results, order, adminConfig.descriptor()));
            return;
        }
        throw new FcliBugException("Unhandled diagnose source: " + source);
    }

    /** Skip when gRPC did not respond; otherwise run {@code validate} with the next stage order. */
    private void runCredentialStage(List<AviatorDiagnosticStageResult> results, AviatorDiagnosticStage stage,
            IntConsumer validate) {
        var order = results.size() + 1;
        if (!diagnostics.hasGrpcResponse(results)) {
            results.add(AviatorDiagnosticStageResult.warn(order, stage,
                "Credential check skipped",
                "Fix the gRPC connection first", false, emptyEvidence()));
            return;
        }
        validate.accept(order);
    }

    private void validateUserToken(List<AviatorDiagnosticStageResult> results, int order, String aviatorUrl, String token) {
        try {
            var validationResult = tokenValidator.validate(aviatorUrl, token);
            var evidence = emptyEvidence();
            evidence.put("tenantNamePresent", validationResult.tenantName() != null);
            if (validationResult.response().getValid()) {
                results.add(optionalPass(order, AviatorDiagnosticStage.TOKEN,
                    "Aviator token is valid", evidence));
                return;
            }
            var errorMessage = validationResult.response().getErrorMessage();
            if (errorMessage != null && !errorMessage.isBlank()) {
                evidence.put("tokenValidationMessage", errorMessage);
            }
            results.add(AviatorDiagnosticStageResult.optionalFail(order, AviatorDiagnosticStage.TOKEN,
                "Aviator token is not valid",
                "Use a current token for the expected tenant", evidence));
        } catch (Exception e) {
            results.add(AviatorDiagnosticStageResult.optionalFail(order, AviatorDiagnosticStage.TOKEN,
                "Aviator token check failed",
                "Use a current token for the expected tenant",
                AviatorConnectionDiagnostics.errorEvidence(e)));
        }
    }

    private void validateAdminConfig(List<AviatorDiagnosticStageResult> results, int order,
            AviatorAdminConfigDescriptor configDescriptor) {
        try {
            adminValidator.validate(configDescriptor);
            var evidence = emptyEvidence();
            evidence.put("tenant", configDescriptor.getTenant());
            results.add(optionalPass(order, AviatorDiagnosticStage.ADMIN,
                "Aviator admin credentials are valid", evidence));
        } catch (Exception e) {
            results.add(AviatorDiagnosticStageResult.optionalFail(order, AviatorDiagnosticStage.ADMIN,
                "Admin credentials are not valid",
                "Check the tenant, public key, and private key", AviatorConnectionDiagnostics.errorEvidence(e)));
        }
    }

    private static AviatorDiagnosticStageResult optionalPass(int order, AviatorDiagnosticStage stage,
            String summary, ObjectNode evidence) {
        return AviatorDiagnosticStageResult.of(order, stage, AviatorDiagnosticStatus.PASS, false,
            summary, "No action required", evidence);
    }

    private static ObjectNode emptyEvidence() {
        return JsonHelper.getObjectMapper().createObjectNode();
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
