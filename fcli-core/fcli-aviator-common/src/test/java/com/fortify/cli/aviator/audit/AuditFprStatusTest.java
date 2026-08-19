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

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResponse.AuditSkipReason;

class AuditFprStatusTest {

    @Test
    void determinesAuditStatusForSuccessSkipAndFailureCombinations() {
        assertEquals("AUDITED", AuditFPR.determineAuditStatus(3, 0, 3, 3));
        assertEquals("PARTIALLY_AUDITED", AuditFPR.determineAuditStatus(2, 1, 3, 3));
        assertEquals("SKIPPED", AuditFPR.determineAuditStatus(0, 3, 3, 3));
        assertEquals("FAILED", AuditFPR.determineAuditStatus(0, 2, 3, 3));
        assertEquals("FAILED", AuditFPR.determineAuditStatus(0, 3, 3, 2));
    }

    @Test
    void countsOnlyExplicitSkippedResponsesAsSkipped() {
        AuditResponse skipped = AuditResponse.builder()
                .status("SKIPPED")
                .auditSkipReason(AuditSkipReason.SOURCE_FILE_DECODE_FAILED)
                .statusMessage("The source decoder wording can change")
                .build();
        AuditResponse failed = AuditResponse.builder()
                .status("FAILED")
                .statusMessage("backend error")
                .build();
        AuditResponse success = AuditResponse.builder().status("SUCCESS").build();

        assertEquals(Map.of("Source file decode failed", 1),
                AuditFPR.getSkippedAuditReasons(Map.of(
                        "skipped", skipped,
                        "failed", failed,
                        "success", success), 3));
    }

    @Test
    void classifiesLegacyServerMessagesAtTheResponseBoundary() {
        assertEquals(AuditSkipReason.SOURCE_FILE_READ_FAILED,
                AuditSkipReason.from("SKIPPED", "example could not be read from the FPR"));
        assertEquals(AuditSkipReason.SOURCE_FILE_NOT_FOUND,
                AuditSkipReason.from("SKIPPED", "example was not found in the FPR"));
    }

    @Test
    void preservesMissingResponseAccountingForFilteredIssues() {
        AuditResponse success = AuditResponse.builder().status("SUCCESS").build();

        assertEquals(Map.of("No audit response received", 1),
                AuditFPR.getSkippedAuditReasons(Map.of("success", success), 2));
    }

    @Test
    void countsOnlyResponsesOriginatingFromAviatorAsSubmitted() {
        AuditResponse localSkip = AuditResponse.builder().status("SKIPPED").build();
        AuditResponse localFailure = AuditResponse.builder()
                .status("FAILED")
                .statusMessage("Request validation failed")
                .build();
        AuditResponse serverSkip = AuditResponse.builder()
                        .status("SKIPPED")
                        .submittedToAviator(true)
                        .build();
        AuditResponse serverFailure = AuditResponse.builder()
                .status("FAILED")
                .statusMessage("Aviator processing failed")
                .submittedToAviator(true)
                .build();

        assertEquals(2, AuditFPR.getSubmittedAuditCount(Map.of(
                        "local", localSkip,
                "localFailure", localFailure,
                "server", serverSkip,
                "serverFailure", serverFailure)));
    }
}