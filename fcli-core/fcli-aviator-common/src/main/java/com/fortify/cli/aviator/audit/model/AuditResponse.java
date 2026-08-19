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
package com.fortify.cli.aviator.audit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Reflectable
public class AuditResponse {
    public enum AuditSkipReason {
        SOURCE_FILE_DECODE_FAILED(
                "Could not decode source file%s: %s%s",
            "Source file decode failed"),
        SOURCE_FILE_NOT_FOUND(
                "%s was not found in the FPR",
            "Source file not found in FPR"),
        SOURCE_FILE_READ_FAILED(
                "%s could not be read from the FPR%s",
            "Source file read failed"),
        AUDIT_FAILED("FAILED", "Audit failed"),
        SKIPPED_BY_AVIATOR("SKIPPED", "Skipped by Aviator"),
        UNKNOWN(null, "Unknown audit failure"),
        OTHER(null, null);

        private final String messageFormat;
        private final String displayMessage;

        AuditSkipReason(String messageFormat, String displayMessage) {
            this.messageFormat = messageFormat;
            this.displayMessage = displayMessage;
        }

        public String format(Object... args) {
            return String.format(messageFormat, args);
        }

        public String displayMessage(String status, String statusMessage) {
            return displayMessage == null
                    ? statusMessage == null || statusMessage.isBlank() ? status : statusMessage
                    : displayMessage;
        }
    }

    private AuditResult auditResult;
    private int inputToken;
    private int outputToken;

    private String status;
    private String statusMessage;
    @JsonIgnore
    private AuditSkipReason auditSkipReason;
    private String issueId;
    @JsonIgnore
    private boolean submittedToAviator;

    private String tier;
    private String aviatorPredictionTag;
    private Boolean isAviatorProcessed;
    private String userPrompt;
    private String systemPrompt;

    @JsonIgnore
    public AuditSkipReason getAuditSkipReason() {
        return auditSkipReason == null ? AuditSkipReason.UNKNOWN : auditSkipReason;
    }

    public AuditResponse(AuditResult auditResult, int inputToken, int outputToken, String status,
                         String statusMessage, String issueId, String tier, String aviatorPredictionTag,
                         Boolean isAviatorProcessed, String userPrompt, String systemPrompt) {
        this.auditResult = auditResult;
        this.inputToken = inputToken;
        this.outputToken = outputToken;
        this.status = status;
        this.statusMessage = statusMessage;
        this.issueId = issueId;
        this.tier = tier;
        this.aviatorPredictionTag = aviatorPredictionTag;
        this.isAviatorProcessed = isAviatorProcessed;
        this.userPrompt = userPrompt;
        this.systemPrompt = systemPrompt;
    }

}
