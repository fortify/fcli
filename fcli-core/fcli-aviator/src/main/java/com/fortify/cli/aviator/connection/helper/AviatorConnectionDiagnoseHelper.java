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
import com.fortify.cli.aviator.diagnose.AviatorConnectionDiagnostics;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStage;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStageResult;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStatus;
import com.fortify.cli.common.json.JsonHelper;

public class AviatorConnectionDiagnoseHelper {
    private final AviatorConnectionDiagnostics diagnostics;

    public AviatorConnectionDiagnoseHelper() {
        this(new AviatorConnectionDiagnostics());
    }

    /** For tests that inject transport diagnostics with a fake probe. */
    public AviatorConnectionDiagnoseHelper(AviatorConnectionDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public DiagnoseRunResult diagnose(AviatorConnectionDiagnoseSource source, int timeoutSeconds) {
        var results = diagnostics.diagnose(source.url(), timeoutSeconds, source.type());
        appendCredentialStage(results, source);
        return new DiagnoseRunResult(results, diagnostics.toArrayNode(results), diagnostics.hasRequiredFailure(results));
    }

    private void appendCredentialStage(List<AviatorDiagnosticStageResult> results, AviatorConnectionDiagnoseSource source) {
        if (!source.hasCredentials()) {
            // Optional work that was never requested: omit the stage entirely.
            return;
        }
        var order = results.size() + 1;
        var stage = stageForSource(source);
        if (!diagnostics.hasGrpcResponse(results)) {
            results.add(AviatorDiagnosticStageResult.warn(order, stage,
                "Credential check skipped",
                "Fix the gRPC connection first", false, JsonHelper.getObjectMapper().createObjectNode()));
            return;
        }
        if (source.hasAdminConfig()) {
            validateAdminConfig(results, order, source.adminConfigDescriptor());
        } else {
            validateUserToken(results, order, source.url(), resolveToken(source));
        }
    }

    private void validateUserToken(List<AviatorDiagnosticStageResult> results, int order, String aviatorUrl, String token) {
        try {
            var validationResult = AviatorUserSessionHelper.instance().validateToken(aviatorUrl, token);
            var evidence = JsonHelper.getObjectMapper().createObjectNode();
            evidence.put("tenantNamePresent", validationResult.tenantName() != null);
            if (validationResult.response().getValid()) {
                results.add(AviatorDiagnosticStageResult.of(order, AviatorDiagnosticStage.TOKEN,
                    AviatorDiagnosticStatus.PASS, false,
                    "Aviator token is valid", "No action required", evidence));
            } else {
                var errorMessage = validationResult.response().getErrorMessage();
                if (errorMessage != null && !errorMessage.isBlank()) {
                    evidence.put("tokenValidationMessage", errorMessage);
                }
                results.add(AviatorDiagnosticStageResult.optionalFail(order, AviatorDiagnosticStage.TOKEN,
                    "Aviator token is not valid",
                    "Use a current token for the expected tenant", evidence));
            }
        } catch (Exception e) {
            // Optional stage: always report, never abort the diagnose run.
            results.add(AviatorDiagnosticStageResult.optionalFail(order, AviatorDiagnosticStage.TOKEN,
                "Aviator token check failed",
                "Use a current token for the expected tenant",
                AviatorConnectionDiagnostics.errorEvidence(e)));
        }
    }

    private void validateAdminConfig(List<AviatorDiagnosticStageResult> results, int order,
            AviatorAdminConfigDescriptor configDescriptor) {
        try {
            AviatorAdminConfigHelper.instance().validateConfig(configDescriptor);
            var evidence = JsonHelper.getObjectMapper().createObjectNode();
            evidence.put("tenant", configDescriptor.getTenant());
            results.add(AviatorDiagnosticStageResult.of(order, AviatorDiagnosticStage.ADMIN,
                AviatorDiagnosticStatus.PASS, false,
                "Aviator admin credentials are valid", "No action required", evidence));
        } catch (Exception e) {
            // Optional stage: always report, never abort the diagnose run.
            results.add(AviatorDiagnosticStageResult.optionalFail(order, AviatorDiagnosticStage.ADMIN,
                "Admin credentials are not valid",
                "Check the tenant, public key, and private key", AviatorConnectionDiagnostics.errorEvidence(e)));
        }
    }

    private static String resolveToken(AviatorConnectionDiagnoseSource source) {
        if (source.userSessionDescriptor() != null) {
            return source.userSessionDescriptor().getAviatorToken();
        }
        return source.rawToken();
    }

    private static AviatorDiagnosticStage stageForSource(AviatorConnectionDiagnoseSource source) {
        return source.hasAdminConfig() ? AviatorDiagnosticStage.ADMIN : AviatorDiagnosticStage.TOKEN;
    }

    public record DiagnoseRunResult(List<AviatorDiagnosticStageResult> stages, ArrayNode json, boolean requiredFailure) {}
}
