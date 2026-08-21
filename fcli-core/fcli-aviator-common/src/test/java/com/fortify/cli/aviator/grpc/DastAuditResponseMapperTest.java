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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import com.fortify.aviator.dastaudit.DastAuditDecision;
import com.fortify.aviator.dastaudit.DastAuditResponse;

class DastAuditResponseMapperTest {
    @Test
    void usesIssueIdAssociatedWithRequest() {
        var response = DastAuditResponse.newBuilder()
            .setRequestId("request-1")
            .setDastIssueId("DAST-1")
            .setStatus("SUCCESS")
            .setDecision(DastAuditDecision.newBuilder().setTruePositive(true).setConfidence("HIGH"))
            .build();

        DastAuditResult result = DastAuditResponseMapper.map(response, "DAST-1");

        var success = assertInstanceOf(DastAuditResult.Success.class, result);
        assertEquals("DAST-1", result.issueId());
        assertEquals("SUCCESS", result.status());
        assertEquals(true, success.truePositive());
    }

    @Test
    void rejectsMismatchedServerIssueId() {
        var response = DastAuditResponse.newBuilder()
            .setRequestId("request-1")
            .setDastIssueId("DAST-WRONG")
            .setStatus("SUCCESS")
            .build();

        DastAuditResult result = DastAuditResponseMapper.map(response, "DAST-1");

        assertInstanceOf(DastAuditResult.Failure.class, result);
        assertEquals("DAST-1", result.issueId());
        assertEquals("FAILED", result.status());
    }

    @Test
    void preservesSkippedResponseAsDistinctVariant() {
        var response = DastAuditResponse.newBuilder()
            .setRequestId("request-1")
            .setDastIssueId("DAST-1")
            .setStatus("SKIPPED")
            .setStatusMessage("Quota exceeded")
            .build();

        DastAuditResult result = DastAuditResponseMapper.map(response, "DAST-1");

        assertInstanceOf(DastAuditResult.Skipped.class, result);
        assertEquals("SKIPPED", result.status());
        assertEquals("Quota exceeded", result.statusMessage());
    }
}