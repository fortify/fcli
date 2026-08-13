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

import java.io.File;
import java.util.Map;

import lombok.Data;

@Data
public class FPRAuditResult {
    private File updatedFile;
    private String status;
    private String message;
    private int issuesSuccessfullyAudited;
    private int totalIssuesToAudit;
    private int issuesSubmitted;
    private int issuesSkipped;
    private Map<String, Integer> skippedByReason;
    private int remediationGenerationSkipped;
    private Map<String, Integer> remediationGenerationSkippedByReason;

    public FPRAuditResult(File updatedFile, String status, String message,
                        int issuesSuccessfullyAudited, int totalIssuesToAudit) {
        this(updatedFile, status, message, issuesSuccessfullyAudited, totalIssuesToAudit,
            totalIssuesToAudit, Math.max(0, totalIssuesToAudit - issuesSuccessfullyAudited), Map.of(), 0, Map.of());
    }

    public FPRAuditResult(File updatedFile, String status, String message,
                        int issuesSuccessfullyAudited, int totalIssuesToAudit, int issuesSkipped,
                        Map<String, Integer> skippedByReason, int remediationGenerationSkipped,
                        Map<String, Integer> remediationGenerationSkippedByReason) {
        this(updatedFile, status, message, issuesSuccessfullyAudited, totalIssuesToAudit, totalIssuesToAudit,
            issuesSkipped, skippedByReason, remediationGenerationSkipped, remediationGenerationSkippedByReason);
    }

    public FPRAuditResult(File updatedFile, String status, String message,
                        int issuesSuccessfullyAudited, int totalIssuesToAudit, int issuesSubmitted,
                        int issuesSkipped, Map<String, Integer> skippedByReason, int remediationGenerationSkipped,
                        Map<String, Integer> remediationGenerationSkippedByReason) {
        this.updatedFile = updatedFile;
        this.status = status;
        this.message = message;
        this.issuesSuccessfullyAudited = issuesSuccessfullyAudited;
        this.totalIssuesToAudit = totalIssuesToAudit;
        this.issuesSubmitted = issuesSubmitted;
        this.issuesSkipped = issuesSkipped;
        this.skippedByReason = skippedByReason == null ? Map.of() : Map.copyOf(skippedByReason);
        this.remediationGenerationSkipped = remediationGenerationSkipped;
        this.remediationGenerationSkippedByReason = remediationGenerationSkippedByReason == null ? Map.of() : Map.copyOf(remediationGenerationSkippedByReason);
    }
}