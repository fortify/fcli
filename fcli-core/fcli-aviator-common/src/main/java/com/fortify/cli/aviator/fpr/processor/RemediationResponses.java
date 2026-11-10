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
package com.fortify.cli.aviator.fpr.processor;

import java.util.List;

import com.fortify.cli.aviator.audit.model.AuditResponse;

/**
 * What an audit response says about its remediation: whether it has changes of its own, and the issues the
 * server reports as covering it or as superseded by it.
 */
final class RemediationResponses {
    private RemediationResponses() {
    }

    static boolean hasRemediationChanges(AuditResponse response) {
        return response != null && response.getAuditResult() != null
                && response.getAuditResult().getAutoremediation() != null
                && response.getAuditResult().getAutoremediation().getChanges() != null
                && !response.getAuditResult().getAutoremediation().getChanges().isEmpty();
    }

    /** Issue ids whose own fix the server reports as contained in this issue's fix. */
    static List<String> supersededIssueIds(AuditResponse response) {
        if (!hasRemediationChanges(response)) {
            return List.of();
        }
        List<String> ids = response.getAuditResult().getAutoremediation().getSupersededIssueIds();
        return ids == null ? List.of() : ids.stream().filter(id -> id != null && !id.isBlank()).toList();
    }

    /** Issue ids whose prior fix already covers this issue; empty when the issue has its own changes or none apply. */
    static List<String> coveredByPriorFixIds(AuditResponse response) {
        if (response == null || response.getAuditResult() == null
                || response.getAuditResult().getAutoremediation() == null
                || hasRemediationChanges(response)) {
            return List.of();
        }
        List<String> ids = response.getAuditResult().getAutoremediation().getCoveredByIssueIds();
        return ids == null ? List.of() : ids.stream().filter(id -> id != null && !id.isBlank()).toList();
    }
}
