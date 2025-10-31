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