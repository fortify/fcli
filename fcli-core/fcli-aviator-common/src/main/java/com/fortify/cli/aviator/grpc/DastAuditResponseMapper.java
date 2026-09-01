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
package com.fortify.cli.aviator.grpc;

import com.fortify.aviator.dastaudit.DastAuditResponse;
import com.fortify.cli.aviator.audit.model.AuditTier;

/**
 * Maps a DAST audit response to the issue associated with its request ID.
 */
final class DastAuditResponseMapper {
    private DastAuditResponseMapper() {}

    static DastAuditResult map(DastAuditResponse response, String expectedIssueId) {
        String responseIssueId = response.getDastIssueId();
        if (!responseIssueId.isBlank() && !expectedIssueId.equals(responseIssueId)) {
            return DastAuditResult.Failure.builder()
                .issueId(expectedIssueId)
                .status("FAILED")
                .statusMessage("DAST audit response issue ID mismatch: expected '" + expectedIssueId
                    + "' but received '" + responseIssueId + "'")
                .build();
        }

        if ("SKIPPED".equalsIgnoreCase(response.getStatus())) {
            return DastAuditResult.Skipped.builder()
                .issueId(expectedIssueId)
                .statusMessage(response.getStatusMessage())
                .build();
        }
        if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
            return DastAuditResult.Failure.builder()
                .issueId(expectedIssueId)
                .status(response.getStatus())
                .statusMessage(response.getStatusMessage())
                .build();
        }
        if (!response.hasDecision()) {
            return DastAuditResult.Failure.builder()
                .issueId(expectedIssueId)
                .status("FAILED")
                .statusMessage("Successful DAST audit response did not contain a decision")
                .build();
        }

        var decision = response.getDecision();
        return DastAuditResult.Success.builder()
            .issueId(expectedIssueId)
            .truePositive(decision.getTruePositive())
            .confidence(decision.getConfidence())
            .reasoning(decision.getReasoning())
            .remediationAdvice(decision.getRemediationAdvice())
            .finalComment(decision.getFinalComment())
            .tagValue(decision.getTagValue())
            .tier(AuditTier.fromServerValue(decision.getTier()))
            .build();
    }
}
