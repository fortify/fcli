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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;

/**
 * Tests for edge cases in RemediationProcessor and RemediationMetric.
 */
class RemediationProcessorEdgeCasesTest {

    @Test
    void emptyRemediationsReturnsEmptyPreviewDetails() {
        RemediationMetric metric = RemediationMetric.unfiltered(0, 0, Set.of(), java.util.Map.of(), java.util.List.of());
        
        assertNotNull(metric);
        assertEquals(0, metric.totalRemediations());
        assertEquals(0, metric.appliedRemediations());
        assertNotNull(metric.previewDetails());
        assertEquals(0, metric.previewDetails().size());
    }

    @Test
    void nonExistentIssueIdIsTrackedAsRequested() {
        Set<String> requestedIds = Set.of("ISSUE-1", "NONEXISTENT-123");
        Set<String> appliedIds = Set.of("ISSUE-1");
        
        RemediationMetric metric = RemediationMetric.filtered(requestedIds, appliedIds, Set.of("file.java"));
        
        assertNotNull(metric);
        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations());
        assertEquals(1, metric.skippedRemediations());
        assertTrue(metric.requestedIssueIds().contains("NONEXISTENT-123"));
    }

    @Test
    void filteredMetricWithAllIdsAppliedHasNoSkips() {
        Set<String> requestedIds = Set.of("ISSUE-1", "ISSUE-2");
        Set<String> appliedIds = Set.of("ISSUE-1", "ISSUE-2");
        
        RemediationMetric metric = RemediationMetric.filtered(requestedIds, appliedIds, Set.of("file.java"));
        
        assertEquals(2, metric.totalRemediations());
        assertEquals(2, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
    }

    @Test
    void unfilteredMetricWithNoRemediationsHasZeroTotals() {
        RemediationMetric metric = RemediationMetric.unfiltered(0, 0, Set.of());
        
        assertEquals(0, metric.totalRemediations());
        assertEquals(0, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals(0, metric.modifiedFiles().size());
    }

    @Test
    void filteredModeDoesNotIncludeUnfilteredFields() {
        Set<String> requestedIds = Set.of("ISSUE-1");
        Set<String> appliedIds = Set.of("ISSUE-1");
        
        RemediationMetric metric = RemediationMetric.filtered(requestedIds, appliedIds, Set.of());
        
        assertTrue(metric.isFiltered());
        assertNotNull(metric.requestedIssueIds());
        assertNotNull(metric.appliedIssueIds());
    }

    @Test
    void unfilteredModeHasEmptyIssueIdSets() {
        RemediationMetric metric = RemediationMetric.unfiltered(5, 3, Set.of("file.java"));
        
        assertEquals(false, metric.isFiltered());
        assertEquals(0, metric.requestedIssueIds().size());
        assertEquals(0, metric.appliedIssueIds().size());
    }

    @Test
    void previewDetailsCanBeNull() {
        RemediationMetric metric = RemediationMetric.unfiltered(5, 3, Set.of(), java.util.Map.of(), null);
        
        // Null preview details is acceptable (non-preview mode)
        assertEquals(null, metric.previewDetails());
    }
}
