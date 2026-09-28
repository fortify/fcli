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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Reflectable
public class AuditResponse {
    @Getter
    @RequiredArgsConstructor
    public enum AuditSkipReason {
        SOURCE_FILE_DECODE_FAILED(
                "Could not decode source file%s: %s%s",
                "Source file decode failed"),
        SOURCE_FILE_READ_FAILED(
                "%s could not be read from the FPR%s",
                "Source file read failed"),
        SKIPPED_BY_AVIATOR("SKIPPED", "Skipped by Aviator");

        private final String messageFormat;
        private final String displayMessage;

        public String format(Object... args) {
            return String.format(messageFormat, args);
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
}
