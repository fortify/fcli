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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.fpr.remediation.RemediationExecutionMode;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationMetric;
import com.fortify.cli.aviator.fpr.remediation.preview.PreviewDetail;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

/**
 * Shared metric aggregation and result-field helpers for SSC/FoD apply-remediations flows.
 */
public final class AviatorRemediationMetricsHelper {
    private static final String REQUESTED_ISSUE_NOT_FOUND = "Requested issue not found in remediations";

    private AviatorRemediationMetricsHelper() {}

    /**
     * Aggregates per-FPR metrics. {@code requestedIssueIds == null} selects unfiltered
     * aggregation (XML totals); non-null selects filtered aggregation (requested IDs).
     */
    public static RemediationMetric aggregateMetrics(Set<String> requestedIssueIds, Collection<RemediationMetric> metrics) {
        Collection<RemediationMetric> safeMetrics = metrics == null ? List.of() : metrics;
        return requestedIssueIds == null
                ? aggregateUnfiltered(safeMetrics)
                : aggregateFiltered(requestedIssueIds, safeMetrics);
    }

    private static RemediationMetric aggregateUnfiltered(Collection<RemediationMetric> metrics) {
        RemediationMetric.RemediationMetricBuilder builder = RemediationMetric.builder();
        for (RemediationMetric metric : metrics) {
            builder.add(metric);
        }
        return builder.build();
    }

    private static RemediationMetric aggregateFiltered(Set<String> requestedIssueIds, Collection<RemediationMetric> metrics) {
        Set<String> seenIssueIds = new LinkedHashSet<>();
        Set<String> satisfiedIssueIds = new LinkedHashSet<>();
        Set<String> appliedIssueIds = new LinkedHashSet<>();
        Set<String> identicalIssueIds = new LinkedHashSet<>();
        Set<String> supersededIssueIds = new LinkedHashSet<>();
        Set<String> possiblyRemediatedIssueIds = new LinkedHashSet<>();
        Set<String> modifiedFiles = new LinkedHashSet<>();
        Map<String, String> issueSkipReasons = new LinkedHashMap<>();
        Map<String, PreviewDetail> previewDetailsByIssue = new LinkedHashMap<>();
        boolean previewMode = false;
        for (RemediationMetric metric : metrics) {
            seenIssueIds.addAll(metric.seenIssueIds());
            satisfiedIssueIds.addAll(metric.satisfiedIssueIds());
            appliedIssueIds.addAll(metric.appliedIssueIds());
            identicalIssueIds.addAll(metric.identicalIssueIds());
            supersededIssueIds.addAll(metric.supersededIssueIds());
            possiblyRemediatedIssueIds.addAll(metric.possiblyRemediatedIssueIds());
            modifiedFiles.addAll(metric.modifiedFiles());
            issueSkipReasons.putAll(metric.issueSkipReasons());
            previewMode |= metric.isPreview();
            for (PreviewDetail detail : metric.previewDetails()) {
                PreviewDetail existing = previewDetailsByIssue.get(detail.issueId());
                if (existing == null || detail.isAvailable() || !existing.isAvailable()) {
                    previewDetailsByIssue.put(detail.issueId(), detail);
                }
            }
        }

        issueSkipReasons.keySet().removeAll(satisfiedIssueIds);
        possiblyRemediatedIssueIds.removeAll(satisfiedIssueIds);
        for (String requestedIssueId : requestedIssueIds) {
            if (!satisfiedIssueIds.contains(requestedIssueId) && !seenIssueIds.contains(requestedIssueId)) {
                issueSkipReasons.put(requestedIssueId, REQUESTED_ISSUE_NOT_FOUND);
                if (previewMode) {
                    previewDetailsByIssue.put(requestedIssueId,
                        PreviewDetail.skipped(requestedIssueId, null));
                }
            }
        }

        Map<String, Integer> skippedByReason = new LinkedHashMap<>();
        issueSkipReasons.values().forEach(reason -> skippedByReason.merge(reason, 1, Integer::sum));
        int skippedRemediations = requestedIssueIds.size() - satisfiedIssueIds.size();
        return RemediationMetric.builder()
                .totalRemediations(requestedIssueIds.size())
                .appliedRemediations(appliedIssueIds.size())
                .identicalRemediations(identicalIssueIds.size())
                .supersededRemediations(supersededIssueIds.size())
                .possiblyRemediatedRemediations(possiblyRemediatedIssueIds.size())
                .skippedRemediations(skippedRemediations)
                .modifiedFiles(modifiedFiles)
                .skippedByReason(skippedByReason)
                .executionMode(previewMode ? RemediationExecutionMode.PREVIEW : RemediationExecutionMode.APPLY)
                .requestedIssueIds(requestedIssueIds)
                .seenIssueIds(seenIssueIds)
                .satisfiedIssueIds(satisfiedIssueIds)
                .appliedIssueIds(appliedIssueIds)
                .identicalIssueIds(identicalIssueIds)
                .supersededIssueIds(supersededIssueIds)
                .possiblyRemediatedIssueIds(possiblyRemediatedIssueIds)
                .issueSkipReasons(issueSkipReasons)
                .previewDetails(new ArrayList<>(previewDetailsByIssue.values()))
                .build();
    }

    public static Set<String> getRemainingIssueIds(Set<String> requestedIssueIds, RemediationMetric metric) {
        if (requestedIssueIds == null || requestedIssueIds.isEmpty()) {
            return requestedIssueIds;
        }
        Set<String> remainingIssueIds = new LinkedHashSet<>(requestedIssueIds);
        remainingIssueIds.removeAll(metric.satisfiedIssueIds());
        return remainingIssueIds;
    }

    public static void mergeSkippedByReason(Map<String, Integer> target, Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((reason, count) -> target.merge(reason, count, Integer::sum));
    }

    /** Compact table-friendly summary: {@code reason=count, ...}. */
    public static String formatSkippedReasons(Map<String, Integer> skippedByReason) {
        if (skippedByReason == null || skippedByReason.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        skippedByReason.forEach((reason, count) -> parts.add(reason + "=" + count));
        return String.join(", ", parts);
    }

    public static String actionLabel(RemediationMetric metric) {
        boolean previewMode = metric != null && metric.isPreview();
        if (metric != null && metric.appliedRemediations() > 0) {
            return previewMode ? "Remediation-Previewed" : "Remediation-Applied";
        } else {
            return previewMode ? "No-Remediation-Previewed" : "No-Remediation-Applied";
        }
    }

    public static String na(String value) {
        return value != null ? value : "N/A";
    }

    /**
     * Writes always-present remediation metric fields onto a result node
     * (totals, skip reasons, modified files). Does not set {@code __action__}.
     */
    public static void putRemediationMetricFields(ObjectNode result, RemediationMetric metric) {
        int total = metric == null ? 0 : metric.totalRemediations();
        int applied = metric == null ? 0 : metric.appliedRemediations();
        int skipped = metric == null ? 0 : metric.skippedRemediations();
        String appliedFieldName = metric != null && metric.isPreview() ? "availableRemediation" : "appliedRemediation";
        Map<String, Integer> skippedByReason = metric == null ? Map.of() : metric.skippedByReason();
        Set<String> modifiedFiles = metric == null ? Set.of() : metric.modifiedFiles();

        result.put("totalRemediation", total);
        result.put(appliedFieldName, applied);
        result.put("identicalRemediation", metric == null ? 0 : metric.identicalRemediations());
        result.put("supersededRemediation", metric == null ? 0 : metric.supersededRemediations());
        result.put("possiblyRemediatedRemediation", metric == null ? 0 : metric.possiblyRemediatedRemediations());
        result.put("skippedRemediation", skipped);
        result.put("skippedReasons", formatSkippedReasons(skippedByReason));
        result.set("skippedByReason", toObjectNode(skippedByReason));
        result.set("modifiedFiles", toArrayNode(modifiedFiles));
    }

    /** Metric fields plus {@code __action__} and, for preview results, preview details (shared by SSC/FoD result builders). */
    public static void putMetricAndAction(ObjectNode result, RemediationMetric metric) {
        putRemediationMetricFields(result, metric);
        result.put(IActionCommandResultSupplier.actionFieldName, actionLabel(metric));

        if (metric != null && metric.isPreview()) {
            result.set("previewDetails", toPreviewDetailsArray(metric.previewDetails()));
        }
    }
    
    private static ArrayNode toPreviewDetailsArray(List<?> previewDetails) {
        ArrayNode array = JsonHelper.getObjectMapper().createArrayNode();
        if (previewDetails != null) {
            previewDetails.forEach(detail -> array.add(JsonHelper.getObjectMapper().valueToTree(detail)));
        }
        return array;
    }

    /**
     * Cache-mode extras shared by SSC/FoD: file path, entry list, and product id list field.
     *
     * @param idArrayField {@code artifactIds} (SSC) or {@code releaseIds} (FoD)
     */
    public static void putCacheExtras(
            ObjectNode result, Path cacheZip, List<String> entryPaths, String idArrayField, List<String> ids) {
        result.put("file", cacheZip.toString());
        result.set("entries", toStringArrayNode(entryPaths));
        result.set(idArrayField, toStringArrayNode(ids));
    }

    public static ObjectNode toObjectNode(Map<String, Integer> skippedByReason) {
        ObjectNode object = JsonHelper.getObjectMapper().createObjectNode();
        if (skippedByReason != null) {
            skippedByReason.forEach(object::put);
        }
        return object;
    }

    public static ArrayNode toArrayNode(Set<String> files) {
        ArrayNode array = JsonHelper.getObjectMapper().createArrayNode();
        if (files != null) {
            files.forEach(array::add);
        }
        return array;
    }

    public static ArrayNode toStringArrayNode(List<String> values) {
        ArrayNode array = JsonHelper.getObjectMapper().createArrayNode();
        if (values != null) {
            values.forEach(array::add);
        }
        return array;
    }
}
