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
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper;
import com.fortify.cli.aviator.connection.cli.mixin.AviatorConnectionDiagnoseSourceArgGroup;
import com.fortify.cli.aviator.diagnose.AviatorConnectionDiagnostics;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStage;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStageResult;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStatus;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;

import io.grpc.StatusRuntimeException;

public class AviatorConnectionDiagnoseHelper {
    private final AviatorConnectionDiagnostics diagnostics = new AviatorConnectionDiagnostics();

    public List<AviatorDiagnosticStageResult> diagnose(AviatorConnectionDiagnoseSourceArgGroup sourceArgGroup, int timeoutSeconds) {
        var source = resolveSource(sourceArgGroup);
        var results = diagnostics.diagnose(source.url(), timeoutSeconds, source.type());
        appendCredentialStage(results, source);
        return results;
    }

    public ArrayNode toArrayNode(List<AviatorDiagnosticStageResult> results) {
        return diagnostics.toArrayNode(results);
    }

    public boolean hasRequiredFailure(List<AviatorDiagnosticStageResult> results) {
        return diagnostics.hasRequiredFailure(results);
    }

    private DiagnoseSource resolveSource(AviatorConnectionDiagnoseSourceArgGroup sourceArgGroup) {
        if (sourceArgGroup.getUrl() != null) {
            return new DiagnoseSource("url", sourceArgGroup.getUrl(), null, null);
        }
        if (sourceArgGroup.getAviatorSession() != null) {
            var descriptor = AviatorUserSessionHelper.instance().get(sourceArgGroup.getAviatorSession(), true);
            return new DiagnoseSource("user-session", descriptor.getAviatorUrl(), descriptor, null);
        }
        var descriptor = AviatorAdminConfigHelper.instance().get(sourceArgGroup.getAdminConfig(), true);
        return new DiagnoseSource("admin-config", descriptor.getAviatorUrl(), null, descriptor);
    }

    private void appendCredentialStage(List<AviatorDiagnosticStageResult> results, DiagnoseSource source) {
        var order = results.size() + 1;
        if (source.userSessionDescriptor() == null && source.adminConfigDescriptor() == null) {
            results.add(AviatorConnectionDiagnostics.warn(order, AviatorDiagnosticStage.TOKEN,
                "Credential check skipped",
                "Use --aviator-session or --admin-config to check credentials", false,
                JsonHelper.getObjectMapper().createObjectNode()));
            return;
        }
        if (!diagnostics.hasGrpcResponse(results)) {
            results.add(AviatorConnectionDiagnostics.warn(order, stageForSource(source),
                "Credential check skipped",
                "Fix the gRPC connection first", false, JsonHelper.getObjectMapper().createObjectNode()));
            return;
        }
        if (source.userSessionDescriptor() != null) {
            validateUserToken(results, order, source.userSessionDescriptor());
        } else {
            validateAdminConfig(results, order, source.adminConfigDescriptor());
        }
    }

    private void validateUserToken(List<AviatorDiagnosticStageResult> results, int order, AviatorUserSessionDescriptor sessionDescriptor) {
        try {
            var validationResult = AviatorUserSessionHelper.instance().validateToken(sessionDescriptor);
            var evidence = JsonHelper.getObjectMapper().createObjectNode();
            evidence.put("tenantNamePresent", validationResult.tenantName() != null);
            var errorMessage = validationResult.response().getErrorMessage();
            if (validationResult.response().getValid()) {
                results.add(AviatorDiagnosticStageResult.of(order, AviatorDiagnosticStage.TOKEN,
                    AviatorDiagnosticStatus.PASS, false,
                    "Aviator token is valid", "No action required", evidence));
            } else {
                if (errorMessage != null && !errorMessage.isBlank()) {
                    evidence.put("tokenValidationMessage", errorMessage);
                }
                results.add(AviatorConnectionDiagnostics.optionalFail(order, AviatorDiagnosticStage.TOKEN,
                    "Aviator token is not valid",
                    "Use a current token for the expected tenant", evidence));
            }
        } catch (AviatorSimpleException | AviatorTechnicalException | StatusRuntimeException e) {
            results.add(AviatorConnectionDiagnostics.optionalFail(order, AviatorDiagnosticStage.TOKEN,
                "Aviator token check failed",
                "Use a current token for the expected tenant",
                AviatorConnectionDiagnostics.errorEvidence(e)));
        }
    }

    private void validateAdminConfig(List<AviatorDiagnosticStageResult> results, int order, AviatorAdminConfigDescriptor configDescriptor) {
        try {
            AviatorAdminConfigHelper.instance().validateConfig(configDescriptor);
            var evidence = JsonHelper.getObjectMapper().createObjectNode();
            evidence.put("tenant", configDescriptor.getTenant());
            results.add(AviatorDiagnosticStageResult.of(order, AviatorDiagnosticStage.ADMIN,
                AviatorDiagnosticStatus.PASS, false,
                "Aviator admin credentials are valid", "No action required", evidence));
        } catch (FcliSimpleException | AviatorTechnicalException | StatusRuntimeException e) {
            results.add(AviatorConnectionDiagnostics.optionalFail(order, AviatorDiagnosticStage.ADMIN,
                "Admin credentials are not valid",
                "Check the tenant, public key, and private key", AviatorConnectionDiagnostics.errorEvidence(e)));
        }
    }

    private AviatorDiagnosticStage stageForSource(DiagnoseSource source) {
        return source.adminConfigDescriptor() == null ? AviatorDiagnosticStage.TOKEN : AviatorDiagnosticStage.ADMIN;
    }

    private record DiagnoseSource(String type, String url, AviatorUserSessionDescriptor userSessionDescriptor,
            AviatorAdminConfigDescriptor adminConfigDescriptor) {}
}