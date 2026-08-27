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
package com.fortify.cli.aviator.audit;

import java.util.Locale;

import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResult;
import com.fortify.cli.aviator.grpc.DastAuditResult;
import com.fortify.cli.aviator.util.Constants;

/**
 * Converts structured DAST decisions to conservative FCLI audit results.
 */
public final class DastAuditDecisionMapper {
    private DastAuditDecisionMapper() {}

    public static AuditResponse toAuditResponse(DastAuditResult result) {
        if (!(result instanceof DastAuditResult.Success success)) {
            return AuditResponse.builder()
            .issueId(result.issueId())
            .status(result.status())
            .statusMessage(result.statusMessage())
                .build();
        }

        String confidence = normalizedConfidence(success.confidence());
        String tagValue;
        String prediction;
        String tier;
        if (success.truePositive()) {
            tagValue = Constants.EXPLOITABLE;
            prediction = Constants.AVIATOR_REMEDIATION_REQUIRED;
            tier = "GOLD";
        } else if ("HIGH".equals(confidence)) {
            tagValue = Constants.NOT_AN_ISSUE;
            prediction = Constants.AVIATOR_NOT_AN_ISSUE;
            tier = "GOLD";
        } else {
            tagValue = Constants.NOT_AN_ISSUE;
            prediction = Constants.AVIATOR_LIKELY_FP;
            tier = "SILVER";
        }

        String comment = success.finalComment() != null && !success.finalComment().isBlank()
            ? success.finalComment()
            : success.reasoning();
        return AuditResponse.builder()
            .issueId(success.issueId())
            .status("SUCCESS")
            .tier(tier)
            .aviatorPredictionTag(prediction)
            .isAviatorProcessed(true)
            .auditResult(AuditResult.builder().tagValue(tagValue).comment(comment).build())
            .build();
    }

    private static String normalizedConfidence(String confidence) {
        if (confidence == null) return "LOW";
        String normalized = confidence.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "HIGH", "MEDIUM", "LOW" -> normalized;
            default -> "LOW";
        };
    }
}