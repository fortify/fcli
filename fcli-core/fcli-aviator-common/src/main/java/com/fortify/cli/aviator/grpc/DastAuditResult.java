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

/** Domain representation of one terminal DAST audit response. */
public sealed interface DastAuditResult permits DastAuditResult.Success, DastAuditResult.Skipped, DastAuditResult.Failure {
    String issueId();
    String status();
    String statusMessage();

    record Success(
        String issueId,
        boolean truePositive,
        String confidence,
        String reasoning,
        String remediationAdvice,
        String finalComment,
        String tagValue,
        String tier
    ) implements DastAuditResult {
        @Override
        public String status() {
            return "SUCCESS";
        }

        @Override
        public String statusMessage() {
            return "";
        }
    }

    record Skipped(String issueId, String statusMessage) implements DastAuditResult {
        @Override
        public String status() {
            return "SKIPPED";
        }
    }

    record Failure(String issueId, String status, String statusMessage) implements DastAuditResult {}
}