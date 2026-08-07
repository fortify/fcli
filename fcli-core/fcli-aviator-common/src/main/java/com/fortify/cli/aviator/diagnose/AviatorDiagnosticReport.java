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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

/**
 * Owns stage order, collects diagnostic rows, and emits support log lines for one diagnose run.
 * <p>
 * Call {@link #begin(AviatorDiagnosticStage)} (or the string overload) before running a stage;
 * {@code pass}/{@code fail}/{@code warn}/{@code skip*} record the row and log the outcome.
 * Skip APIs take a structured {@code reason} so log formatting never parses summary English.
 */
public final class AviatorDiagnosticReport {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorDiagnosticReport.class);

    private final List<AviatorDiagnosticStageResult> stages = new ArrayList<>();

    public int nextOrder() {
        return stages.size() + 1;
    }

    /** DEBUG start line before a stage is executed (not for pure skip rows). */
    public void begin(AviatorDiagnosticStage stage) {
        LOG.debug("Starting {} diagnostic", stage.displayName());
    }

    /** DEBUG start line for product stages (token/admin) that are not transport enums. */
    public void begin(String stageId) {
        LOG.debug("Starting {} diagnostic", AviatorDiagnosticStage.displayNameFor(stageId));
    }

    public void add(AviatorDiagnosticStageResult result) {
        stages.add(result);
        logStageOutcome(result, false, null);
    }

    private void addSkip(AviatorDiagnosticStageResult result, String reason) {
        stages.add(result);
        logStageOutcome(result, true, reason);
    }

    private static void logStageOutcome(AviatorDiagnosticStageResult result, boolean skipped, String skipReason) {
        var name = AviatorDiagnosticStage.displayNameFor(result.stage());
        if (skipped) {
            LOG.info("{} skipped because {}", name, nullToEmpty(skipReason));
            return;
        }
        switch (result.status()) {
        case PASS -> LOG.info("{}: PASS - {}", name, nullToEmpty(result.summary()));
        case FAIL -> LOG.error("{}: FAIL - {}", name, failDetail(result));
        case WARN -> LOG.info("{}: WARN - {}", name, nullToEmpty(result.summary()));
        }
    }

    private static String failDetail(AviatorDiagnosticStageResult result) {
        var summary = nullToEmpty(result.summary());
        var evidence = result.evidence();
        if (evidence == null || !evidence.hasNonNull("exceptionMessage")) {
            return summary;
        }
        var exceptionMessage = evidence.get("exceptionMessage").asText();
        if (exceptionMessage == null || exceptionMessage.isBlank()) {
            return summary;
        }
        return summary+" ("+exceptionMessage+")";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public void pass(AviatorDiagnosticStage stage, String summary, String guidance, ObjectNode evidence) {
        add(AviatorDiagnosticStageResult.pass(nextOrder(), stage, summary, guidance, evidence));
    }

    public void fail(AviatorDiagnosticStage stage, String summary, String guidance, ObjectNode evidence) {
        add(AviatorDiagnosticStageResult.fail(nextOrder(), stage, summary, guidance, evidence));
    }

    /**
     * Required transport skip WARN. Builds summary {@code Skipped because {reason}} and logs
     * as a skip without parsing summary text.
     */
    public void skipWarn(AviatorDiagnosticStage stage, String reason, String guidance, ObjectNode evidence) {
        addSkip(AviatorDiagnosticStageResult.warn(nextOrder(), stage, skipSummary(reason), guidance, true, evidence),
            reason);
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

    /**
     * Optional product-stage skip WARN. Builds summary {@code Skipped because {reason}}.
     */
    public void optionalSkipWarn(String stage, String description, String reason, String guidance, ObjectNode evidence) {
        addSkip(AviatorDiagnosticStageResult.warn(nextOrder(), stage, description, skipSummary(reason), guidance, false,
            evidence), reason);
    }

    private static String skipSummary(String reason) {
        return "Skipped because "+reason;
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
