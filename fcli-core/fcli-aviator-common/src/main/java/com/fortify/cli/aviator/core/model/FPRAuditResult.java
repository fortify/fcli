package com.fortify.cli.aviator.core.model;

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