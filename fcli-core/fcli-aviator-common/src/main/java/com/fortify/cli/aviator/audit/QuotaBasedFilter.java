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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.util.StringUtil;

/**
 * Utility class for filtering UserPrompts based on quota constraints.
 * Supports priority-based filtering with default or custom priority ordering.
 */
public class QuotaBasedFilter {

    /**
     * Default priority ordering: Critical > High > Medium > Low
     */
    private static final Map<String, Integer> DEFAULT_PRIORITY_ORDER = Map.of(
        "Critical", 4,
        "High", 3,
        "Medium", 2,
        "Low", 1
    );

    /**
     * Filters UserPrompts to fit within the specified quota based on priority.
     * Uses default priority ordering (Critical > High > Medium > Low).
     *
     * @param prompts List of UserPrompts to filter
     * @param quota Maximum number of prompts to retain
     * @return Filtered list of UserPrompts, sorted by priority (highest first)
     */
    public static List<UserPrompt> filterByQuota(List<UserPrompt> prompts, long quota) {
        return filterByQuota(prompts, quota, null);
    }

    /**
     * Filters UserPrompts to fit within the specified quota based on priority.
     * Supports custom priority ordering provided by the client.
     *
     * @param prompts List of UserPrompts to filter
     * @param quota Maximum number of prompts to retain
     * @param customPriorityOrder Custom priority ordering map (priority name -> rank).
     *                           Higher rank values indicate higher priority.
     *                           If null, uses default ordering.
     *                           When custom priority order is provided, prompts with priorities
     *                           not in the custom order are excluded from filtering.
     *                           This may result in fewer prompts than the quota limit.
     * @return Filtered list of UserPrompts, sorted by priority (highest first)
     */
    public static List<UserPrompt> filterByQuota(List<UserPrompt> prompts, long quota,
                                                 Map<String, Integer> customPriorityOrder) {
        if (prompts == null || prompts.isEmpty()) {
            return List.of();
        }

        if (quota <= 0) {
            return List.of();
        }

        boolean useCustomPriorityOrder = (customPriorityOrder != null && !customPriorityOrder.isEmpty());

        Map<String, Integer> priorityOrder = useCustomPriorityOrder
            ? customPriorityOrder
            : DEFAULT_PRIORITY_ORDER;

        // When custom priority order is provided, filter out prompts with unknown priorities
        List<UserPrompt> eligiblePrompts = prompts;
        if (useCustomPriorityOrder) {
            eligiblePrompts = prompts.stream()
                .filter(prompt -> {
                    String priority = extractPriority(prompt);
                    return priorityOrder.containsKey(priority);
                })
                .collect(Collectors.toList());
        }

        if (eligiblePrompts.size() <= quota) {
            return eligiblePrompts;
        }

        Comparator<UserPrompt> priorityComparator = createPriorityComparator(priorityOrder);

        return eligiblePrompts.stream()
                .sorted(priorityComparator)
                .limit(quota)
                .collect(Collectors.toList());
    }

    /**
     * Creates a comparator for UserPrompts based on priority ordering.
     * Prompts with unknown priorities are ranked lowest.
     *
     * @param priorityOrder Map of priority names to rank values
     * @return Comparator that sorts UserPrompts by priority (descending)
     */
    private static Comparator<UserPrompt> createPriorityComparator(Map<String, Integer> priorityOrder) {
        return (a, b) -> {
            String aPriority = extractPriority(a);
            String bPriority = extractPriority(b);

            int aRank = priorityOrder.getOrDefault(aPriority, 0);
            int bRank = priorityOrder.getOrDefault(bPriority, 0);

            // Sort in descending order (highest priority first)
            int comparison = Integer.compare(bRank, aRank);

            // If priorities are equal, maintain stable sort by instanceID
            if (comparison == 0) {
                String aId = a.getIssueData().getInstanceID();
                String bId = b.getIssueData().getInstanceID();
                if (aId != null && bId != null) {
                    return aId.compareTo(bId);
                }
            }

            return comparison;
        };
    }

    /**
     * Extracts priority from UserPrompt.
     * Handles null or empty priority values gracefully.
     *
     * @param prompt UserPrompt to extract priority from
     * @return Priority string, or "Unknown" if not available
     */
    private static String extractPriority(UserPrompt prompt) {
        if (prompt == null || prompt.getIssueData() == null) {
            return "Unknown";
        }

        String priority = prompt.getIssueData().getPriority();
        return StringUtil.isEmpty(priority) ? "Unknown" : priority;
    }

    /**
     * Builds a custom priority order map from a list of priority names.
     * The order in the list determines the rank (first = highest priority).
     *
     * @param orderedPriorities List of priority names in descending order of importance
     * @return Map of priority names to rank values
     */
    public static Map<String, Integer> buildCustomPriorityOrder(List<String> orderedPriorities) {
        if (orderedPriorities == null || orderedPriorities.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Integer> priorityOrder = new HashMap<>();
        int rank = orderedPriorities.size();

        for (String priority : orderedPriorities) {
            if (!StringUtil.isEmpty(priority)) {
                priorityOrder.put(priority, rank);
                rank--;
            }
        }

        return priorityOrder;
    }

    /**
     * Returns the default priority ordering used by the filter.
     *
     * @return Map of default priority names to rank values
     */
    public static Map<String, Integer> getDefaultPriorityOrder() {
        return new HashMap<>(DEFAULT_PRIORITY_ORDER);
    }
}
