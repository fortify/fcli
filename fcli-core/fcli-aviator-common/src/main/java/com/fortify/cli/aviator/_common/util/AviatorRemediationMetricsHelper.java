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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;

/**
 * Shared metric aggregation helpers for SSC/FoD apply-remediations flows.
 */
public final class AviatorRemediationMetricsHelper {
    private AviatorRemediationMetricsHelper() {}

    public static RemediationMetric aggregateMetrics(Set<String> requestedIssueIds, Collection<RemediationMetric> metrics) {
        Set<String> modifiedFiles = new LinkedHashSet<>();
        Map<String, Integer> skippedByReason = new LinkedHashMap<>();
        if (requestedIssueIds == null) {
            int totalRemediations = 0;
            int appliedRemediations = 0;
            for (RemediationMetric metric : metrics) {
                totalRemediations += metric.totalRemediations();
                appliedRemediations += metric.appliedRemediations();
                modifiedFiles.addAll(metric.modifiedFiles());
                mergeSkippedByReason(skippedByReason, metric.skippedByReason());
            }
            return RemediationMetric.unfiltered(totalRemediations, appliedRemediations, modifiedFiles, skippedByReason);
        }
        Set<String> appliedIssueIds = new LinkedHashSet<>();
        for (RemediationMetric metric : metrics) {
            modifiedFiles.addAll(metric.modifiedFiles());
            appliedIssueIds.addAll(metric.appliedIssueIds());
            mergeSkippedByReason(skippedByReason, metric.skippedByReason());
        }
        return RemediationMetric.filtered(requestedIssueIds, appliedIssueIds, modifiedFiles, skippedByReason);
    }

    public static Set<String> getRemainingIssueIds(Set<String> requestedIssueIds, RemediationMetric metric) {
        if (requestedIssueIds == null || requestedIssueIds.isEmpty()) {
            return requestedIssueIds;
        }
        Set<String> remainingIssueIds = new LinkedHashSet<>(requestedIssueIds);
        remainingIssueIds.removeAll(metric.appliedIssueIds());
        return remainingIssueIds;
    }

    public static void mergeSkippedByReason(Map<String, Integer> target, Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((reason, count) -> target.merge(reason, count, Integer::sum));
    }
}
