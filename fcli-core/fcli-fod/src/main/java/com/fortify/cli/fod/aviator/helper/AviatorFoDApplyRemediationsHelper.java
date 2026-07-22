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
package com.fortify.cli.fod.aviator.helper;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper.ApplyResult;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import lombok.Builder;

/**
 * Helper for FoD apply-remediations result JSON construction.
 */
public final class AviatorFoDApplyRemediationsHelper {
    private AviatorFoDApplyRemediationsHelper() {}

    /** Online apply outcome (success or soft-skip) → structured result node. */
    public static ObjectNode buildOnlineResultNode(FoDReleaseDescriptor releaseDescriptor, ApplyResult applyResult) {
        if (applyResult.metrics().isEmpty()) {
            return toOnlineJson(OnlineResultData.builder()
                    .releaseDescriptor(releaseDescriptor)
                    .totalRemediation(0)
                    .appliedRemediation(0)
                    .skippedRemediation(0)
                    .modifiedFiles(Set.of())
                    .action(RemediationsApplyHelper.actionLabel(null))
                    .build());
        }
        RemediationMetric metric = applyResult.metrics().get(0);
        return toOnlineJson(OnlineResultData.builder()
                .releaseDescriptor(releaseDescriptor)
                .totalRemediation(metric.totalRemediations())
                .appliedRemediation(metric.appliedRemediations())
                .skippedRemediation(metric.skippedRemediations())
                .modifiedFiles(metric.modifiedFiles())
                .action(RemediationsApplyHelper.actionLabel(metric))
                .build());
    }

    /**
     * Aggregates metrics and builds the cache apply result JSON (same fields as before).
     */
    public static ObjectNode buildCacheResultNode(
            Path cacheZip, ApplyResult applyResult, Set<String> issueIdFilter) {
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                issueIdFilter, applyResult.metrics());
        return toCacheJson(CacheResultData.builder()
                .cacheZip(cacheZip)
                .entryPaths(applyResult.processedEntries())
                .releaseIds(applyResult.processedIds())
                .totalRemediation(aggregated.totalRemediations())
                .appliedRemediation(aggregated.appliedRemediations())
                .skippedRemediation(aggregated.skippedRemediations())
                .modifiedFiles(aggregated.modifiedFiles())
                .action(RemediationsApplyHelper.actionLabel(aggregated))
                .build());
    }

    private static ObjectNode toOnlineJson(OnlineResultData data) {
        FoDReleaseDescriptor rd = data.releaseDescriptor();
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("releaseId", rd.getReleaseId());
        result.put("applicationName", rd.getApplicationName());
        result.put("releaseName", rd.getReleaseName());
        result.put("totalRemediation", data.totalRemediation());
        result.put("appliedRemediation", data.appliedRemediation());
        result.put("skippedRemediation", data.skippedRemediation());
        result.set("modifiedFiles", toArrayNode(data.modifiedFiles()));
        result.put(IActionCommandResultSupplier.actionFieldName, data.action());
        return result;
    }

    private static ObjectNode toCacheJson(CacheResultData resultData) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("releaseId", resultData.releaseIds() != null && !resultData.releaseIds().isEmpty()
                ? resultData.releaseIds().get(0) : "N/A");
        result.put("applicationName", "N/A");
        result.put("releaseName", "N/A");
        result.put("file", resultData.cacheZip().toString());
        result.put("totalRemediation", resultData.totalRemediation());
        result.put("appliedRemediation", resultData.appliedRemediation());
        result.put("skippedRemediation", resultData.skippedRemediation());
        result.set("modifiedFiles", toArrayNode(resultData.modifiedFiles()));
        result.set("entries", toStringArrayNode(resultData.entryPaths()));
        result.set("releaseIds", toStringArrayNode(resultData.releaseIds()));
        result.put(IActionCommandResultSupplier.actionFieldName, resultData.action());
        return result;
    }

    @Builder
    private record OnlineResultData(
            FoDReleaseDescriptor releaseDescriptor,
            int totalRemediation,
            int appliedRemediation,
            int skippedRemediation,
            Set<String> modifiedFiles,
            String action) {}

    @Builder
    private record CacheResultData(
            Path cacheZip,
            List<String> entryPaths,
            List<String> releaseIds,
            int totalRemediation,
            int appliedRemediation,
            int skippedRemediation,
            Set<String> modifiedFiles,
            String action) {}

    private static ArrayNode toArrayNode(Set<String> files) {
        ArrayNode array = JsonHelper.getObjectMapper().createArrayNode();
        if (files != null) {
            files.forEach(array::add);
        }
        return array;
    }

    private static ArrayNode toStringArrayNode(List<String> values) {
        ArrayNode array = JsonHelper.getObjectMapper().createArrayNode();
        if (values != null) {
            values.forEach(array::add);
        }
        return array;
    }
}
