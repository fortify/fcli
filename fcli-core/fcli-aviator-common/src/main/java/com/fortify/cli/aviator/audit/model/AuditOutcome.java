package com.fortify.cli.aviator.audit.model;

import java.util.Map;

import lombok.Data;

@Data
public class AuditOutcome {
    private Map<String, AuditResponse> auditResponses;
    private int totalIssuesToAudit;

    public AuditOutcome(Map<String, AuditResponse> auditResponses, int totalIssuesToAudit) {
        this.auditResponses = auditResponses;
        this.totalIssuesToAudit = totalIssuesToAudit;
    }
}