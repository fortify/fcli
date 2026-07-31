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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

/**
 * One diagnostic stage row.
 * <p>
 * {@code required} only affects process exit when {@code status} is {@link AviatorDiagnosticStatus#FAIL}:
 * a required FAIL forces soft exit 1 after the table is written. WARN rows may carry
 * {@code required=true} for documentation of skip chains, but WARN never drives exit code.
 */
public record AviatorDiagnosticStageResult(
        int order,
        String stage,
        String description,
        AviatorDiagnosticStatus status,
        boolean required,
        String summary,
        String guidance,
        ObjectNode evidence) {

    public static AviatorDiagnosticStageResult of(int order, String stage, String description,
            AviatorDiagnosticStatus status, boolean required, String summary, String guidance, ObjectNode evidence) {
        return new AviatorDiagnosticStageResult(order, stage, description, status, required, summary, guidance,
            evidence == null ? JsonHelper.getObjectMapper().createObjectNode() : evidence);
    }

    public static AviatorDiagnosticStageResult of(int order, AviatorDiagnosticStage stage,
            AviatorDiagnosticStatus status, boolean required, String summary, String guidance, ObjectNode evidence) {
        return of(order, stage.id(), stage.description(), status, required, summary, guidance, evidence);
    }

    public static AviatorDiagnosticStageResult pass(int order, AviatorDiagnosticStage stage, String summary,
            String guidance, ObjectNode evidence) {
        return of(order, stage, AviatorDiagnosticStatus.PASS, true, summary, guidance, evidence);
    }

    public static AviatorDiagnosticStageResult fail(int order, AviatorDiagnosticStage stage, String summary,
            String guidance, ObjectNode evidence) {
        return of(order, stage, AviatorDiagnosticStatus.FAIL, true, summary, guidance, evidence);
    }

    public static AviatorDiagnosticStageResult warn(int order, AviatorDiagnosticStage stage, String summary,
            String guidance, boolean required, ObjectNode evidence) {
        return of(order, stage, AviatorDiagnosticStatus.WARN, required, summary, guidance, evidence);
    }

    public static AviatorDiagnosticStageResult warn(int order, String stage, String description, String summary,
            String guidance, boolean required, ObjectNode evidence) {
        return of(order, stage, description, AviatorDiagnosticStatus.WARN, required, summary, guidance, evidence);
    }

    public static AviatorDiagnosticStageResult optionalPass(int order, String stage, String description,
            String summary, String guidance, ObjectNode evidence) {
        return of(order, stage, description, AviatorDiagnosticStatus.PASS, false, summary, guidance, evidence);
    }

    public static AviatorDiagnosticStageResult optionalFail(int order, String stage, String description,
            String summary, String guidance, ObjectNode evidence) {
        return of(order, stage, description, AviatorDiagnosticStatus.FAIL, false, summary, guidance, evidence);
    }

    public boolean isRequiredFailure() {
        return required && AviatorDiagnosticStatus.FAIL.equals(status);
    }

    public boolean isStage(AviatorDiagnosticStage stage) {
        return stage != null && stage.id().equals(this.stage);
    }

    public ObjectNode asObjectNode() {
        var node = JsonHelper.getObjectMapper().createObjectNode();
        node.put("order", order);
        node.put("stage", stage);
        node.put("description", description);
        node.put("status", status.name());
        node.put("required", required);
        node.put("summary", summary);
        node.put("guidance", guidance);
        node.set("evidence", evidence);
        return node;
    }
}
