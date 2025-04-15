package com.fortify.cli.aviator.core.model;

import lombok.Data;

import java.util.Map;

@Data
public class AuditOutcome {
    private Map<String, AuditResponse> auditResponses;
    private int totalIssuesToAudit;

    public AuditOutcome(Map<String, AuditResponse> auditResponses, int totalIssuesToAudit) {
        this.auditResponses = auditResponses;
        this.totalIssuesToAudit = totalIssuesToAudit;
    }
}