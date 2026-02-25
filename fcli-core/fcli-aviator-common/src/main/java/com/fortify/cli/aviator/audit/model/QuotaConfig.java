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
package com.fortify.cli.aviator.audit.model;

import java.util.List;
import java.util.Map;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for quota-based filtering of issues.
 */
@Getter
@Builder
@Reflectable
public class QuotaConfig {
    /**
     * Available quota for processing issues. If null or <= 0, no quota limit is applied.
     */
    private final Long availableQuota;

    /**
     * Custom priority ordering for filtering. Map of priority name to rank.
     * Higher rank values indicate higher priority.
     * If null or empty, default ordering (Critical > High > Medium > Low) is used.
     */
    private final Map<String, Integer> customPriorityOrder;

    /**
     * Ordered list of priority names (alternative to customPriorityOrder).
     * First element has highest priority. If provided, this will be converted
     * to a priority order map internally.
     */
    private final List<String> orderedPriorities;

    /**
     * Checks if quota filtering should be applied.
     *
     * @return true if quota is set and greater than 0
     */
    public boolean hasQuota() {
        return availableQuota != null && availableQuota > 0;
    }

    /**
     * Checks if custom priority ordering is configured.
     *
     * @return true if custom priority order or ordered priorities are set
     */
    public boolean hasCustomPriorityOrder() {
        return (customPriorityOrder != null && !customPriorityOrder.isEmpty()) ||
               (orderedPriorities != null && !orderedPriorities.isEmpty());
    }

    /**
     * Creates a QuotaConfig with no quota limit.
     *
     * @return QuotaConfig with unlimited quota
     */
    public static QuotaConfig noQuota() {
        return QuotaConfig.builder().build();
    }

    /**
     * Creates a QuotaConfig with the specified quota and default priority ordering.
     *
     * @param quota Available quota
     * @return QuotaConfig with specified quota
     */
    public static QuotaConfig withQuota(long quota) {
        return QuotaConfig.builder().availableQuota(quota).build();
    }

    /**
     * Creates a QuotaConfig with the specified quota and custom priority ordering.
     *
     * @param quota Available quota
     * @param customPriorityOrder Custom priority order map
     * @return QuotaConfig with quota and custom ordering
     */
    public static QuotaConfig withCustomOrder(long quota, Map<String, Integer> customPriorityOrder) {
        return QuotaConfig.builder()
                .availableQuota(quota)
                .customPriorityOrder(customPriorityOrder)
                .build();
    }

    /**
     * Creates a QuotaConfig with the specified quota and ordered priority list.
     *
     * @param quota Available quota
     * @param orderedPriorities Ordered list of priority names (highest first)
     * @return QuotaConfig with quota and ordered priorities
     */
    public static QuotaConfig withOrderedPriorities(long quota, List<String> orderedPriorities) {
        return QuotaConfig.builder()
                .availableQuota(quota)
                .orderedPriorities(orderedPriorities)
                .build();
    }
}
