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

import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResult;
import com.fortify.cli.aviator.audit.model.AuditTier;
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

        AuditTier tier = success.tier();
        boolean tierOne = tier == AuditTier.GOLD;
        String tagValue;
        String prediction;
        if (success.truePositive()) {
            tagValue = Constants.EXPLOITABLE;
            prediction = tierOne ? Constants.AVIATOR_REMEDIATION_REQUIRED : Constants.AVIATOR_LIKELY_TP;
        } else {
            tagValue = Constants.NOT_AN_ISSUE;
            prediction = tierOne ? Constants.AVIATOR_NOT_AN_ISSUE : Constants.AVIATOR_LIKELY_FP;
        }

        String comment = success.finalComment() != null && !success.finalComment().isBlank()
            ? success.finalComment()
            : success.reasoning();
        return AuditResponse.builder()
            .issueId(success.issueId())
            .status("SUCCESS")
            .tier(tier.name())
            .aviatorPredictionTag(prediction)
            .isAviatorProcessed(true)
            .auditResult(AuditResult.builder().tagValue(tagValue).comment(comment).build())
            .build();
    }

}