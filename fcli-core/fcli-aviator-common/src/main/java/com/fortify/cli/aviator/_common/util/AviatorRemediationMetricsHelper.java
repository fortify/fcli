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
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

/**
 * Shared metric aggregation and result-field helpers for SSC/FoD apply-remediations flows.
 */
public final class AviatorRemediationMetricsHelper {
    private AviatorRemediationMetricsHelper() {}

    /**
     * Aggregates per-FPR metrics. {@code requestedIssueIds == null} selects unfiltered
     * aggregation (XML totals); non-null selects filtered aggregation (requested IDs).
     */
    public static RemediationMetric aggregateMetrics(Set<String> requestedIssueIds, Collection<RemediationMetric> metrics) {
        Set<String> modifiedFiles = new LinkedHashSet<>();
        Map<String, Integer> skippedByReason = new LinkedHashMap<>();
        List<com.fortify.cli.aviator.fpr.processor.preview.PreviewDetail> allPreviewDetails = new ArrayList<>();
        Collection<RemediationMetric> safeMetrics = metrics == null ? List.of() : metrics;
        
        // Aggregate preview details from all metrics
        for (RemediationMetric metric : safeMetrics) {
            if (metric.previewDetails() != null) {
                allPreviewDetails.addAll(metric.previewDetails());
            }
        }
        
        if (requestedIssueIds == null) {
            int totalRemediations = 0;
            int appliedRemediations = 0;
            for (RemediationMetric metric : safeMetrics) {
                totalRemediations += metric.totalRemediations();
                appliedRemediations += metric.appliedRemediations();
                accumulateFilesAndSkips(metric, modifiedFiles, skippedByReason);
            }
            return RemediationMetric.unfiltered(totalRemediations, appliedRemediations, modifiedFiles, skippedByReason, 
                    allPreviewDetails.isEmpty() ? null : allPreviewDetails);
        }
        Set<String> appliedIssueIds = new LinkedHashSet<>();
        for (RemediationMetric metric : safeMetrics) {
            appliedIssueIds.addAll(metric.appliedIssueIds());
            accumulateFilesAndSkips(metric, modifiedFiles, skippedByReason);
        }
        return RemediationMetric.filtered(requestedIssueIds, appliedIssueIds, modifiedFiles, skippedByReason,
                allPreviewDetails.isEmpty() ? null : allPreviewDetails);
    }

    private static void accumulateFilesAndSkips(
            RemediationMetric metric, Set<String> modifiedFiles, Map<String, Integer> skippedByReason) {
        modifiedFiles.addAll(metric.modifiedFiles());
        mergeSkippedByReason(skippedByReason, metric.skippedByReason());
    }

    public static Set<String> getRemainingIssueIds(Set<String> requestedIssueIds, RemediationMetric metric) {
        if (requestedIssueIds == null || requestedIssueIds.isEmpty()) {
            return requestedIssueIds;
        }
        Set<String> remainingIssueIds = new LinkedHashSet<>(requestedIssueIds);
        remainingIssueIds.removeAll(metric.appliedIssueIds());
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
        return metric != null && metric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
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
        Map<String, Integer> skippedByReason = metric == null ? Map.of() : metric.skippedByReason();
        Set<String> modifiedFiles = metric == null ? Set.of() : metric.modifiedFiles();

        result.put("totalRemediation", total);
        result.put("appliedRemediation", applied);
        result.put("skippedRemediation", skipped);
        result.put("skippedReasons", formatSkippedReasons(skippedByReason));
        result.set("skippedByReason", toObjectNode(skippedByReason));
        result.set("modifiedFiles", toArrayNode(modifiedFiles));
    }

    /** Metric fields plus {@code __action__} (shared by SSC/FoD result builders). */
    public static void putMetricAndAction(ObjectNode result, RemediationMetric metric) {
        putMetricAndAction(result, metric, false);
    }

    /** Metric fields plus {@code __action__} and optional preview details (shared by SSC/FoD result builders). */
    public static void putMetricAndAction(ObjectNode result, RemediationMetric metric, boolean previewMode) {
        putRemediationMetricFields(result, metric);
        result.put(IActionCommandResultSupplier.actionFieldName, actionLabel(metric));
        
        if (previewMode && metric != null && metric.previewDetails() != null) {
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
