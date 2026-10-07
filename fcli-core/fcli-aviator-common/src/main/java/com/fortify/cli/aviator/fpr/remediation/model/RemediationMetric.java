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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fortify.cli.aviator.fpr.remediation.RemediationExecutionMode;
import com.fortify.cli.aviator.fpr.remediation.preview.PreviewDetail;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class RemediationMetric {
    private final int totalRemediations;
    private final int appliedRemediations;
    private final int identicalRemediations;
    private final int supersededRemediations;
    private final int possiblyRemediatedRemediations;
    private final int skippedRemediations;
    private final Set<String> modifiedFiles;
    private final Map<String, Integer> skippedByReason;
    private final RemediationExecutionMode executionMode;
    private final Set<String> requestedIssueIds;
    private final Set<String> seenIssueIds;
    private final Set<String> satisfiedIssueIds;
    private final Set<String> appliedIssueIds;
    private final Set<String> identicalIssueIds;
    private final Set<String> supersededIssueIds;
    private final Set<String> possiblyRemediatedIssueIds;
    private final Map<String, String> issueSkipReasons;
    private final List<PreviewDetail> previewDetails;

    @Builder
    private RemediationMetric(
            int totalRemediations,
            int appliedRemediations,
            int identicalRemediations,
            int supersededRemediations,
            int possiblyRemediatedRemediations,
            int skippedRemediations,
            Set<String> modifiedFiles,
            Map<String, Integer> skippedByReason,
            RemediationExecutionMode executionMode,
            Set<String> requestedIssueIds,
            Set<String> seenIssueIds,
            Set<String> satisfiedIssueIds,
            Set<String> appliedIssueIds,
            Set<String> identicalIssueIds,
            Set<String> supersededIssueIds,
            Set<String> possiblyRemediatedIssueIds,
            Map<String, String> issueSkipReasons,
            List<PreviewDetail> previewDetails) {
        this.totalRemediations = totalRemediations;
        this.appliedRemediations = appliedRemediations;
        this.identicalRemediations = identicalRemediations;
        this.supersededRemediations = supersededRemediations;
        this.possiblyRemediatedRemediations = possiblyRemediatedRemediations;
        this.skippedRemediations = skippedRemediations;
        this.modifiedFiles = immutableSet(modifiedFiles);
        this.skippedByReason = immutableMap(skippedByReason);
        this.executionMode = executionMode == null ? RemediationExecutionMode.APPLY : executionMode;
        this.requestedIssueIds = immutableSet(requestedIssueIds);
        this.seenIssueIds = immutableSet(seenIssueIds);
        this.satisfiedIssueIds = immutableSet(satisfiedIssueIds);
        this.appliedIssueIds = immutableSet(appliedIssueIds);
        this.identicalIssueIds = immutableSet(identicalIssueIds);
        this.supersededIssueIds = immutableSet(supersededIssueIds);
        this.possiblyRemediatedIssueIds = immutableSet(possiblyRemediatedIssueIds);
        this.issueSkipReasons = immutableStringMap(issueSkipReasons);
        this.previewDetails = previewDetails == null ? List.of() : List.copyOf(previewDetails);
    }

    public boolean isPreview() {
        return executionMode == RemediationExecutionMode.PREVIEW;
    }

    public boolean isFiltered() {
        return !requestedIssueIds.isEmpty();
    }

    private static Set<String> immutableSet(Set<String> values) {
        return values == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    private static Map<String, Integer> immutableMap(Map<String, Integer> values) {
        return values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static Map<String, String> immutableStringMap(Map<String, String> values) {
        return values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static final class RemediationMetricBuilder {
        public RemediationMetricBuilder() {
            this.modifiedFiles = new LinkedHashSet<>();
            this.skippedByReason = new LinkedHashMap<>();
            this.requestedIssueIds = new LinkedHashSet<>();
            this.seenIssueIds = new LinkedHashSet<>();
            this.satisfiedIssueIds = new LinkedHashSet<>();
            this.appliedIssueIds = new LinkedHashSet<>();
            this.identicalIssueIds = new LinkedHashSet<>();
            this.supersededIssueIds = new LinkedHashSet<>();
            this.possiblyRemediatedIssueIds = new LinkedHashSet<>();
            this.issueSkipReasons = new LinkedHashMap<>();
            this.previewDetails = new ArrayList<>();
        }

        public RemediationMetricBuilder add(RemediationMetric metric) {
            this.totalRemediations += metric.totalRemediations;
            this.appliedRemediations += metric.appliedRemediations;
            this.identicalRemediations += metric.identicalRemediations;
            this.supersededRemediations += metric.supersededRemediations;
            this.possiblyRemediatedRemediations += metric.possiblyRemediatedRemediations;
            this.skippedRemediations += metric.skippedRemediations;
            this.modifiedFiles.addAll(metric.modifiedFiles());
            metric.skippedByReason().forEach((reason, count) ->
                this.skippedByReason.merge(reason, count, Integer::sum));
            this.requestedIssueIds.addAll(metric.requestedIssueIds());
            this.seenIssueIds.addAll(metric.seenIssueIds());
            this.satisfiedIssueIds.addAll(metric.satisfiedIssueIds());
            this.appliedIssueIds.addAll(metric.appliedIssueIds());
            this.identicalIssueIds.addAll(metric.identicalIssueIds());
            this.supersededIssueIds.addAll(metric.supersededIssueIds());
            this.possiblyRemediatedIssueIds.addAll(metric.possiblyRemediatedIssueIds());
            this.issueSkipReasons.putAll(metric.issueSkipReasons());
            this.previewDetails.addAll(metric.previewDetails());
            if (metric.isPreview()) {
                this.executionMode = RemediationExecutionMode.PREVIEW;
            }
            return this;
        }
    }
}
