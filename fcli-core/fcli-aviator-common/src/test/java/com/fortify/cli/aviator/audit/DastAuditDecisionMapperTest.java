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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.grpc.DastAuditResult;
import com.fortify.cli.aviator.util.Constants;

class DastAuditDecisionMapperTest {
    @Test
    void unknownConfidenceFalsePositiveRemainsUnsuppressed() {
        var result = DastAuditResult.Success.builder()
            .issueId("DAST-1")
            .confidence("UNKNOWN")
            .reasoning("reason")
            .finalComment("comment")
            .tagValue("bad")
            .tier("GOLD")
            .build();

        var response = DastAuditDecisionMapper.toAuditResponse(result);

        assertEquals("SILVER", response.getTier());
        assertEquals(Constants.AVIATOR_LIKELY_FP, response.getAviatorPredictionTag());
    }

    @Test
    void highConfidenceFalsePositiveIsSuppressible() {
        var result = DastAuditResult.Success.builder()
            .issueId("DAST-1")
            .confidence("HIGH")
            .reasoning("reason")
            .finalComment("comment")
            .build();

        var response = DastAuditDecisionMapper.toAuditResponse(result);

        assertEquals("GOLD", response.getTier());
        assertEquals(Constants.AVIATOR_NOT_AN_ISSUE, response.getAviatorPredictionTag());
    }
}