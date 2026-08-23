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

import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric.Mode;

class AviatorRemediationMetricsHelperTest {

    @Test
    void aggregateFilteredMetricsDeduplicatesIssueIdsAcrossEntries() {
        RemediationMetric metricOne = RemediationMetric.filtered(
                Set.of("ISSUE-1", "ISSUE-2"), Set.of("ISSUE-1"), Set.of("A.java"));
        RemediationMetric metricTwo = RemediationMetric.filtered(
                Set.of("ISSUE-1", "ISSUE-2"), Set.of("ISSUE-2"), Set.of("B.java"));

        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                Set.of("ISSUE-1", "ISSUE-2"), List.of(metricOne, metricTwo));

        assertTrue(aggregated.isFiltered());
        assertEquals(Mode.FILTERED, aggregated.mode());
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
        RemediationMetric metricOne = RemediationMetric.unfiltered(2, 1, Set.of("A.java"), reasonsOne);
        RemediationMetric metricTwo = RemediationMetric.unfiltered(1, 0, Set.of(), reasonsTwo);

        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                null, List.of(metricOne, metricTwo));

        assertFalse(aggregated.isFiltered());
        assertEquals(Mode.UNFILTERED, aggregated.mode());
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
        RemediationMetric metric = RemediationMetric.filtered(
                Set.of("ISSUE-1", "ISSUE-2"), Set.of("ISSUE-1"), Set.of("A.java"));

        assertEquals(
                Set.of("ISSUE-2"),
                AviatorRemediationMetricsHelper.getRemainingIssueIds(Set.of("ISSUE-1", "ISSUE-2"), metric));
    }

    @Test
    void aggregatingAnyPreviewMetricYieldsPreviewResultWithMergedDetails() {
        RemediationMetric applied = RemediationMetric.unfiltered(1, 1, Set.of("A.java"));
        RemediationMetric preview = RemediationMetric.previewUnfiltered(1, 0, Set.of(), Map.of(),
                List.of(com.fortify.cli.aviator.fpr.processor.preview.PreviewDetail.skipped("ISSUE-2", "Source file missing")));

        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                null, List.of(applied, preview));

        assertTrue(aggregated instanceof RemediationMetric.Preview);
        assertEquals(1, ((RemediationMetric.Preview) aggregated).previewDetails().size());
    }

    @Test
    void aggregatingOnlyAppliedMetricsYieldsAppliedResult() {
        RemediationMetric metricOne = RemediationMetric.unfiltered(1, 1, Set.of("A.java"));
        RemediationMetric metricTwo = RemediationMetric.unfiltered(1, 0, Set.of());

        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                null, List.of(metricOne, metricTwo));

        assertTrue(aggregated instanceof RemediationMetric.Applied);
    }
}
