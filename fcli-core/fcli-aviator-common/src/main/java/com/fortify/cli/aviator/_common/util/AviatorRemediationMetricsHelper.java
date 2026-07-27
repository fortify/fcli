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
package com.fortify.cli.aviator._common.util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;

/**
 * Shared metric aggregation helpers for SSC/FoD apply-remediations flows.
 */
public final class AviatorRemediationMetricsHelper {
    private AviatorRemediationMetricsHelper() {}

    public static RemediationMetric aggregateMetrics(Set<String> requestedIssueIds, Collection<RemediationMetric> metrics) {
        Set<String> modifiedFiles = new LinkedHashSet<>();
        if (requestedIssueIds == null) {
            int totalRemediations = 0;
            int appliedRemediations = 0;
            for (RemediationMetric metric : metrics) {
                totalRemediations += metric.totalRemediations();
                appliedRemediations += metric.appliedRemediations();
                modifiedFiles.addAll(metric.modifiedFiles());
            }
            return RemediationMetric.unfiltered(totalRemediations, appliedRemediations, modifiedFiles);
        }
        Set<String> appliedIssueIds = new LinkedHashSet<>();
        for (RemediationMetric metric : metrics) {
            modifiedFiles.addAll(metric.modifiedFiles());
            appliedIssueIds.addAll(metric.appliedIssueIds());
        }
        return RemediationMetric.filtered(requestedIssueIds, appliedIssueIds, modifiedFiles);
    }

    public static Set<String> getRemainingIssueIds(Set<String> requestedIssueIds, RemediationMetric metric) {
        if (requestedIssueIds == null || requestedIssueIds.isEmpty()) {
            return requestedIssueIds;
        }
        Set<String> remainingIssueIds = new LinkedHashSet<>(requestedIssueIds);
        remainingIssueIds.removeAll(metric.appliedIssueIds());
        return remainingIssueIds;
    }
}
