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
package com.fortify.cli.aviator.fpr.remediation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.remediation.RemediationExecutionMode;
import com.fortify.cli.aviator.fpr.remediation.preview.PreviewDetail;

class RemediationMetricTest {
    @Test
    void builderDefaultsToApplyModeAndEmptyCollections() {
        RemediationMetric metric = RemediationMetric.builder().build();

        assertFalse(metric.isPreview());
        assertFalse(metric.isFiltered());
        assertEquals(RemediationExecutionMode.APPLY, metric.executionMode());
        assertEquals(Set.of(), metric.modifiedFiles());
        assertEquals(List.of(), metric.previewDetails());
    }

    @Test
    void filteredMetricPreservesRequestedAndAppliedIssueIds() {
        RemediationMetric metric = RemediationMetric.builder()
                .totalRemediations(2)
                .appliedRemediations(1)
                .skippedRemediations(1)
                .requestedIssueIds(Set.of("ISSUE-1", "ISSUE-404"))
                .appliedIssueIds(Set.of("ISSUE-1"))
                .modifiedFiles(Set.of("Example.java"))
                .build();

        assertTrue(metric.isFiltered());
        assertEquals(Set.of("ISSUE-1", "ISSUE-404"), metric.requestedIssueIds());
        assertEquals(Set.of("ISSUE-1"), metric.appliedIssueIds());
        assertEquals(1, metric.skippedRemediations());
    }

    @Test
    void previewMetricCarriesTopLevelPreviewDetails() {
        PreviewDetail detail = PreviewDetail.skipped("ISSUE-1", null, "Source file missing");
        RemediationMetric metric = RemediationMetric.builder()
                .executionMode(RemediationExecutionMode.PREVIEW)
                .previewDetails(List.of(detail))
                .build();

        assertTrue(metric.isPreview());
        assertEquals(List.of(detail), metric.previewDetails());
    }
}