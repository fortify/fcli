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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;

class AviatorRemediationMetricsHelperTest {

    @Test
    void aggregateFilteredMetricsDeduplicatesIssueIdsAcrossEntries() {
        RemediationMetric metricOne = RemediationMetric.filtered(
                Set.of("ISSUE-1", "ISSUE-2"), Set.of("ISSUE-1"), Set.of("A.java"));
        RemediationMetric metricTwo = RemediationMetric.filtered(
                Set.of("ISSUE-1", "ISSUE-2"), Set.of("ISSUE-2"), Set.of("B.java"));

        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                Set.of("ISSUE-1", "ISSUE-2"), List.of(metricOne, metricTwo));

        assertEquals(2, aggregated.totalRemediations());
        assertEquals(2, aggregated.appliedRemediations());
        assertEquals(0, aggregated.skippedRemediations());
        assertEquals(Set.of("ISSUE-1", "ISSUE-2"), aggregated.appliedIssueIds());
        assertEquals(Set.of("A.java", "B.java"), aggregated.modifiedFiles());
    }

    @Test
    void remainingIssueIdsDropsAlreadyApplied() {
        RemediationMetric metric = RemediationMetric.filtered(
                Set.of("ISSUE-1", "ISSUE-2"), Set.of("ISSUE-1"), Set.of("A.java"));

        assertEquals(
                Set.of("ISSUE-2"),
                AviatorRemediationMetricsHelper.getRemainingIssueIds(Set.of("ISSUE-1", "ISSUE-2"), metric));
    }
}
