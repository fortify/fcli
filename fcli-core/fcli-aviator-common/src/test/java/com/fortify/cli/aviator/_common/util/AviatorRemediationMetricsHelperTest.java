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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.remediation.RemediationExecutionMode;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationMetric;
import com.fortify.cli.aviator.fpr.remediation.preview.PreviewDetail;

class AviatorRemediationMetricsHelperTest {

    @Test
    void aggregateFilteredMetricsDeduplicatesIssueIdsAcrossEntries() {
        RemediationMetric metricOne = filteredMetric(Set.of("ISSUE-1", "ISSUE-2"), Set.of("ISSUE-1"), Set.of("A.java"));
        RemediationMetric metricTwo = filteredMetric(Set.of("ISSUE-1", "ISSUE-2"), Set.of("ISSUE-2"), Set.of("B.java"));

        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                Set.of("ISSUE-1", "ISSUE-2"), List.of(metricOne, metricTwo));

        assertTrue(aggregated.isFiltered());
        assertEquals(2, aggregated.totalRemediations());
        assertEquals(2, aggregated.appliedRemediations());
        assertEquals(0, aggregated.skippedRemediations());
        assertEquals(Set.of("ISSUE-1", "ISSUE-2"), aggregated.appliedIssueIds());
        assertEquals(Set.of("A.java", "B.java"), aggregated.modifiedFiles());
    }

    @Test
    void aggregateUnfilteredMergesSkippedByReason() {
        Map<String, Integer> reasonsOne = new LinkedHashMap<>();
        reasonsOne.put("Source file missing", 1);
        Map<String, Integer> reasonsTwo = new LinkedHashMap<>();
        reasonsTwo.put("Source file missing", 1);
        reasonsTwo.put("No file changes found", 1);
        RemediationMetric metricOne = unfilteredMetric(2, 1, Set.of("A.java"), reasonsOne);
        RemediationMetric metricTwo = unfilteredMetric(1, 0, Set.of(), reasonsTwo);

        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                null, List.of(metricOne, metricTwo));

        assertFalse(aggregated.isFiltered());
        assertEquals(3, aggregated.totalRemediations());
        assertEquals(1, aggregated.appliedRemediations());
        assertEquals(2, aggregated.skippedRemediations());
        assertEquals(2, aggregated.skippedByReason().get("Source file missing"));
        assertEquals(1, aggregated.skippedByReason().get("No file changes found"));
        assertEquals("Source file missing=2, No file changes found=1",
                AviatorRemediationMetricsHelper.formatSkippedReasons(aggregated.skippedByReason()));
    }

    @Test
    void remainingIssueIdsDropsAlreadyApplied() {
                RemediationMetric metric = filteredMetric(Set.of("ISSUE-1", "ISSUE-2"), Set.of("ISSUE-1"), Set.of("A.java"));

        assertEquals(
                Set.of("ISSUE-2"),
                AviatorRemediationMetricsHelper.getRemainingIssueIds(Set.of("ISSUE-1", "ISSUE-2"), metric));
    }

    @Test
    void aggregatingAnyPreviewMetricYieldsPreviewResultWithMergedDetails() {
        RemediationMetric applied = unfilteredMetric(1, 1, Set.of("A.java"), Map.of());
        RemediationMetric preview = RemediationMetric.builder()
                .totalRemediations(1)
                .skippedRemediations(1)
                .executionMode(RemediationExecutionMode.PREVIEW)
                .previewDetails(List.of(PreviewDetail.skipped("ISSUE-2", null)))
                .build();

        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                null, List.of(applied, preview));

        assertTrue(aggregated.isPreview());
        assertEquals(1, aggregated.previewDetails().size());
    }

    @Test
    void skippedPreviewRunWithNoMetricsStaysPreview() {
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                null, List.of(), RemediationExecutionMode.PREVIEW);

        assertTrue(aggregated.isPreview());
        assertEquals(0, aggregated.appliedRemediations());
        assertEquals("No-Remediation-Previewed", AviatorRemediationMetricsHelper.actionLabel(aggregated));
    }

    @Test
    void skippedApplyRunWithNoMetricsStaysApply() {
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                null, List.of(), RemediationExecutionMode.APPLY);

        assertFalse(aggregated.isPreview());
        assertEquals("No-Remediation-Applied", AviatorRemediationMetricsHelper.actionLabel(aggregated));
    }

    @Test
        void aggregatingOnlyAppliedMetricsYieldsApplyResult() {
                RemediationMetric metricOne = unfilteredMetric(1, 1, Set.of("A.java"), Map.of());
                RemediationMetric metricTwo = unfilteredMetric(1, 0, Set.of(), Map.of());

        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                null, List.of(metricOne, metricTwo));

                assertFalse(aggregated.isPreview());
        }

        private RemediationMetric filteredMetric(Set<String> requestedIds, Set<String> appliedIds, Set<String> modifiedFiles) {
                return RemediationMetric.builder()
                                .totalRemediations(requestedIds.size())
                                .appliedRemediations(appliedIds.size())
                                .skippedRemediations(requestedIds.size() - appliedIds.size())
                                .requestedIssueIds(requestedIds)
                                .seenIssueIds(appliedIds)
                                .satisfiedIssueIds(appliedIds)
                                .appliedIssueIds(appliedIds)
                                .modifiedFiles(modifiedFiles)
                                .build();
        }

        private RemediationMetric unfilteredMetric(int total, int applied, Set<String> modifiedFiles,
                        Map<String, Integer> skippedByReason) {
                return RemediationMetric.builder()
                                .totalRemediations(total)
                                .appliedRemediations(applied)
                                .skippedRemediations(total - applied)
                                .modifiedFiles(modifiedFiles)
                                .skippedByReason(skippedByReason)
                                .build();
    }
}
