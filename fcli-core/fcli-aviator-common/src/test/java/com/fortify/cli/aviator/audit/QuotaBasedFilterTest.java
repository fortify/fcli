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
package com.fortify.cli.aviator.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.audit.model.IssueData;
import com.fortify.cli.aviator.audit.model.UserPrompt;

/**
 * Tests for QuotaBasedFilter functionality.
 */
class QuotaBasedFilterTest {

    private List<UserPrompt> testPrompts;

    @BeforeEach
    void setUp() {
        testPrompts = new ArrayList<>();

        // Create test prompts with different priorities
        testPrompts.add(createPrompt("1", "Critical"));
        testPrompts.add(createPrompt("2", "High"));
        testPrompts.add(createPrompt("3", "Medium"));
        testPrompts.add(createPrompt("4", "Low"));
        testPrompts.add(createPrompt("5", "Critical"));
        testPrompts.add(createPrompt("6", "High"));
        testPrompts.add(createPrompt("7", "Medium"));
        testPrompts.add(createPrompt("8", "Low"));
    }

    @Test
    void testFilterByQuota_DefaultOrdering() {
        // Filter to top 3
        List<UserPrompt> filtered = QuotaBasedFilter.filterByQuota(testPrompts, 3);

        assertEquals(3, filtered.size());

        // Should get the 2 Critical prompts first
        assertEquals("Critical", filtered.get(0).getIssueData().getPriority());
        assertEquals("Critical", filtered.get(1).getIssueData().getPriority());

        // Then 1 High priority
        assertEquals("High", filtered.get(2).getIssueData().getPriority());
    }

    @Test
    void testFilterByQuota_NoFilteringNeeded() {
        // Quota larger than list size
        List<UserPrompt> filtered = QuotaBasedFilter.filterByQuota(testPrompts, 100);

        assertEquals(testPrompts.size(), filtered.size());
    }

    @Test
    void testFilterByQuota_ZeroQuota() {
        List<UserPrompt> filtered = QuotaBasedFilter.filterByQuota(testPrompts, 0);

        assertTrue(filtered.isEmpty());
    }

    @Test
    void testFilterByQuota_CustomOrdering() {
        // Custom order: Low > Medium > High > Critical (reverse of default)
        Map<String, Integer> customOrder = Map.of(
            "Low", 4,
            "Medium", 3,
            "High", 2,
            "Critical", 1
        );

        List<UserPrompt> filtered = QuotaBasedFilter.filterByQuota(testPrompts, 3, customOrder);

        assertEquals(3, filtered.size());

        // Should get Low priority prompts first with custom ordering
        assertEquals("Low", filtered.get(0).getIssueData().getPriority());
        assertEquals("Low", filtered.get(1).getIssueData().getPriority());
        assertEquals("Medium", filtered.get(2).getIssueData().getPriority());
    }

    @Test
    void testBuildCustomPriorityOrder() {
        List<String> orderedPriorities = Arrays.asList("Blocker", "Critical", "High", "Medium", "Low");

        Map<String, Integer> priorityOrder = QuotaBasedFilter.buildCustomPriorityOrder(orderedPriorities);

        assertEquals(5, priorityOrder.size());
        assertTrue(priorityOrder.get("Blocker") > priorityOrder.get("Critical"));
        assertTrue(priorityOrder.get("Critical") > priorityOrder.get("High"));
        assertTrue(priorityOrder.get("High") > priorityOrder.get("Medium"));
        assertTrue(priorityOrder.get("Medium") > priorityOrder.get("Low"));
    }

    @Test
    void testBuildCustomPriorityOrder_WithCustomCategories() {
        List<String> orderedPriorities = Arrays.asList("P0", "P1", "P2", "P3");

        Map<String, Integer> priorityOrder = QuotaBasedFilter.buildCustomPriorityOrder(orderedPriorities);

        assertEquals(4, priorityOrder.size());
        assertEquals(4, priorityOrder.get("P0"));
        assertEquals(3, priorityOrder.get("P1"));
        assertEquals(2, priorityOrder.get("P2"));
        assertEquals(1, priorityOrder.get("P3"));
    }

    @Test
    void testFilterWithUnknownPriorities() {
        testPrompts.add(createPrompt("9", null));
        testPrompts.add(createPrompt("10", ""));
        testPrompts.add(createPrompt("11", "Unknown"));

        List<UserPrompt> filtered = QuotaBasedFilter.filterByQuota(testPrompts, 5);

        assertEquals(5, filtered.size());

        // Known priorities should come first
        assertTrue(filtered.stream().limit(2)
                .allMatch(p -> "Critical".equals(p.getIssueData().getPriority())));
    }

    @Test
    void testGetDefaultPriorityOrder() {
        Map<String, Integer> defaultOrder = QuotaBasedFilter.getDefaultPriorityOrder();

        assertEquals(4, defaultOrder.size());
        assertTrue(defaultOrder.get("Critical") > defaultOrder.get("High"));
        assertTrue(defaultOrder.get("High") > defaultOrder.get("Medium"));
        assertTrue(defaultOrder.get("Medium") > defaultOrder.get("Low"));
    }

    private UserPrompt createPrompt(String id, String priority) {
        return UserPrompt.builder()
                .issueData(IssueData.builder()
                        .instanceID(id)
                        .priority(priority)
                        .build())
                .build();
    }
}
