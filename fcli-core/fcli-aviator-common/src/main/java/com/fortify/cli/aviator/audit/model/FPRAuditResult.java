/*
 * Copyright 2021-2025 Open Text.
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

import lombok.Data;

@Data
public class FPRAuditResult {
    private File updatedFile;
    private String status;
    private String message;
    private int issuesSuccessfullyAudited;
    private int totalIssuesToAudit;

    public FPRAuditResult(File updatedFile, String status, String message,
                        int issuesSuccessfullyAudited, int totalIssuesToAudit) {
        this.updatedFile = updatedFile;
        this.status = status;
        this.message = message;
        this.issuesSuccessfullyAudited = issuesSuccessfullyAudited;
        this.totalIssuesToAudit = totalIssuesToAudit;
    }
}