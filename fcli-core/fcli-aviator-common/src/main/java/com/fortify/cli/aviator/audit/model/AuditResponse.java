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
    private AuditResult auditResult;
    private int inputToken;
    private int outputToken;


    private String status;
    private String statusMessage;
    private String issueId;
    @JsonIgnore
    private boolean submittedToAviator;

    private String tier;
    private String aviatorPredictionTag;
    private Boolean isAviatorProcessed;
    private String userPrompt;
    private String systemPrompt;

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
