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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

/**
 * Owns stage order and collects diagnostic rows for one diagnose run.
 */
public final class AviatorDiagnosticReport {
    private final List<AviatorDiagnosticStageResult> stages = new ArrayList<>();

    public int nextOrder() {
        return stages.size() + 1;
    }

    public void add(AviatorDiagnosticStageResult result) {
        stages.add(result);
    }

    public void pass(AviatorDiagnosticStage stage, String summary, String guidance, ObjectNode evidence) {
        add(AviatorDiagnosticStageResult.pass(nextOrder(), stage, summary, guidance, evidence));
    }

    public void fail(AviatorDiagnosticStage stage, String summary, String guidance, ObjectNode evidence) {
        add(AviatorDiagnosticStageResult.fail(nextOrder(), stage, summary, guidance, evidence));
    }

    /**
     * Transport skip WARN. {@code required=true} documents the stage as part of the required
     * pipeline; WARN itself never drives process exit (only required FAIL does).
     */
    public void skipWarn(AviatorDiagnosticStage stage, String summary, String guidance, ObjectNode evidence) {
        add(AviatorDiagnosticStageResult.warn(nextOrder(), stage, summary, guidance, true, evidence));
    }

    public void warn(AviatorDiagnosticStage stage, String summary, String guidance, boolean required,
            ObjectNode evidence) {
        add(AviatorDiagnosticStageResult.warn(nextOrder(), stage, summary, guidance, required, evidence));
    }

    public void optionalPass(String stage, String description, String summary, String guidance, ObjectNode evidence) {
        add(AviatorDiagnosticStageResult.optionalPass(nextOrder(), stage, description, summary, guidance, evidence));
    }

    public void optionalFail(String stage, String description, String summary, String guidance, ObjectNode evidence) {
        add(AviatorDiagnosticStageResult.optionalFail(nextOrder(), stage, description, summary, guidance, evidence));
    }

    public void optionalSkipWarn(String stage, String description, String summary, String guidance, ObjectNode evidence) {
        add(AviatorDiagnosticStageResult.warn(nextOrder(), stage, description, summary, guidance, false, evidence));
    }

    public List<AviatorDiagnosticStageResult> stages() {
        return Collections.unmodifiableList(stages);
    }

    public ArrayNode toArrayNode() {
        var array = JsonHelper.getObjectMapper().createArrayNode();
        stages.stream().map(AviatorDiagnosticStageResult::asObjectNode).forEach(array::add);
        return array;
    }

    public boolean hasRequiredFailure() {
        return stages.stream().anyMatch(AviatorDiagnosticStageResult::isRequiredFailure);
    }

    /** True when the gRPC stage completed with status PASS (server response received). */
    public boolean hasGrpcStagePass() {
        return stages.stream()
            .anyMatch(result -> result.isStage(AviatorDiagnosticStage.GRPC)
                    && AviatorDiagnosticStatus.PASS.equals(result.status()));
    }
}
