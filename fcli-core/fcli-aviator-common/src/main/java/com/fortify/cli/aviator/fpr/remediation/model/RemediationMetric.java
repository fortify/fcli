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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class RemediationMetric {
    private final int totalRemediations;
    private final int appliedRemediations;
    private final int identicalRemediations;
    private final int supersededRemediations;
    private final int possiblyRemediatedRemediations;
    private final int skippedRemediations;
    private final Set<String> modifiedFiles;
    private final Map<String, Integer> skippedByReason;

    public RemediationMetric(int totalRemediations, int appliedRemediations, int identicalRemediations,
                             int supersededRemediations, int possiblyRemediatedRemediations, int skippedRemediations,
                             Set<String> modifiedFiles, Map<String, Integer> skippedByReason) {
        this.totalRemediations = totalRemediations;
        this.appliedRemediations = appliedRemediations;
        this.identicalRemediations = identicalRemediations;
        this.supersededRemediations = supersededRemediations;
        this.possiblyRemediatedRemediations = possiblyRemediatedRemediations;
        this.skippedRemediations = skippedRemediations;
        this.modifiedFiles = modifiedFiles;
        this.skippedByReason = skippedByReason;
    }

    public int totalRemediations() { return totalRemediations; }
    public int appliedRemediations() { return appliedRemediations; }
    public int identicalRemediations() { return identicalRemediations; }
    public int supersededRemediations() { return supersededRemediations; }
    public int possiblyRemediatedRemediations() { return possiblyRemediatedRemediations; }
    public int skippedRemediations() { return skippedRemediations; }
    public Set<String> modifiedFiles() { return modifiedFiles; }
    public Map<String, Integer> skippedByReason() { return skippedByReason; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int totalRemediations;
        private int appliedRemediations;
        private int identicalRemediations;
        private int supersededRemediations;
        private int possiblyRemediatedRemediations;
        private int skippedRemediations;
        private final Set<String> modifiedFiles = new LinkedHashSet<>();
        private final Map<String, Integer> skippedByReason = new HashMap<>();

        public Builder add(RemediationMetric metric) {
            this.totalRemediations += metric.totalRemediations;
            this.appliedRemediations += metric.appliedRemediations;
            this.identicalRemediations += metric.identicalRemediations;
            this.supersededRemediations += metric.supersededRemediations;
            this.possiblyRemediatedRemediations += metric.possiblyRemediatedRemediations;
            this.skippedRemediations += metric.skippedRemediations;
            this.modifiedFiles.addAll(metric.modifiedFiles);
            metric.skippedByReason.forEach((reason, count) ->
                this.skippedByReason.merge(reason, count, Integer::sum));
            return this;
        }

        public RemediationMetric build() {
            return new RemediationMetric(totalRemediations, appliedRemediations, identicalRemediations,
                supersededRemediations, possiblyRemediatedRemediations, skippedRemediations,
                modifiedFiles, skippedByReason);
        }
    }
}
