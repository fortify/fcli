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

import java.util.regex.Pattern;

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
                "Source file decode failed",
                Pattern.compile("^Could not decode source file.*")),
        SOURCE_FILE_NOT_FOUND(
                "%s was not found in the FPR",
                "Source file not found in FPR",
                Pattern.compile(".* was not found in the FPR.*")),
        SOURCE_FILE_READ_FAILED(
                "%s could not be read from the FPR%s",
                "Source file read failed",
                Pattern.compile(".* could not be read from the FPR.*")),
        AUDIT_FAILED("FAILED", "Audit failed", Pattern.compile("^FAILED$", Pattern.CASE_INSENSITIVE)),
        SKIPPED_BY_AVIATOR("SKIPPED", "Skipped by Aviator", Pattern.compile("^SKIPPED$", Pattern.CASE_INSENSITIVE)),
        UNKNOWN(null, "Unknown audit failure", null),
        OTHER(null, null, null);

        private static final String CLIENT_SIDE_ERROR_PREFIX = "Client-side pre-processing error: ";

        private final String messageFormat;
        private final String displayMessage;
        private final Pattern messagePattern;

        AuditSkipReason(String messageFormat, String displayMessage, Pattern messagePattern) {
            this.messageFormat = messageFormat;
            this.displayMessage = displayMessage;
            this.messagePattern = messagePattern;
        }

        public String format(Object... args) {
            return String.format(messageFormat, args);
        }

        public static AuditSkipReason from(String status, String statusMessage) {
            String message = effectiveMessage(status, statusMessage);
            if (message.isBlank()) {
                return UNKNOWN;
            }
            for (AuditSkipReason reason : values()) {
                if (reason.messagePattern != null && reason.messagePattern.matcher(message).matches()) {
                    return reason;
                }
            }
            return OTHER;
        }

        public String displayMessage(String status, String statusMessage) {
            return displayMessage == null ? effectiveMessage(status, statusMessage) : displayMessage;
        }

        private static String effectiveMessage(String status, String statusMessage) {
            String message = statusMessage == null || statusMessage.isBlank() ? status : statusMessage;
            if (message == null || message.isBlank()) {
                return "";
            }
            return message.startsWith(CLIENT_SIDE_ERROR_PREFIX)
                    ? message.substring(CLIENT_SIDE_ERROR_PREFIX.length())
                    : message;
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
        return auditSkipReason == null ? AuditSkipReason.from(status, statusMessage) : auditSkipReason;
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
