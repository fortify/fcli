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

/**
 * Maps a DAST audit response to the issue associated with its request ID.
 */
final class DastAuditResponseMapper {
    private DastAuditResponseMapper() {}

    static DastAuditResult map(DastAuditResponse response, String expectedIssueId) {
        String responseIssueId = response.getDastIssueId();
        if (!responseIssueId.isBlank() && !expectedIssueId.equals(responseIssueId)) {
            return new DastAuditResult.Failure(
                expectedIssueId, "FAILED",
                "DAST audit response issue ID mismatch: expected '" + expectedIssueId
                    + "' but received '" + responseIssueId + "'");
        }

        if ("SKIPPED".equalsIgnoreCase(response.getStatus())) {
            return new DastAuditResult.Skipped(expectedIssueId, response.getStatusMessage());
        }
        if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
            return new DastAuditResult.Failure(
                expectedIssueId, response.getStatus(), response.getStatusMessage());
        }
        if (!response.hasDecision()) {
            return new DastAuditResult.Failure(
                expectedIssueId, "FAILED", "Successful DAST audit response did not contain a decision");
        }

        var decision = response.getDecision();
        return new DastAuditResult.Success(
            expectedIssueId, decision.getTruePositive(), decision.getConfidence(),
            decision.getReasoning(), decision.getRemediationAdvice(), decision.getFinalComment(),
            decision.getTagValue(), decision.getTier());
    }
}
