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
 * <p>
 * Always-present fields live in {@link CommonApplyResult} (online = common only).
 * Cache mode adds path/list properties via {@link CacheResultData#toJson()}.
 */
public final class AviatorFoDApplyRemediationsHelper {
    private AviatorFoDApplyRemediationsHelper() {}

    /** Online apply outcome (success or soft-skip) → always-present result fields only. */
    public static ObjectNode buildOnlineResultNode(FoDReleaseDescriptor releaseDescriptor, ApplyResult applyResult) {
        RemediationMetric metric = applyResult.metrics().isEmpty() ? null : applyResult.metrics().get(0);
        return CommonApplyResult.builder()
                .releaseId(releaseDescriptor.getReleaseId())
                .applicationName(releaseDescriptor.getApplicationName())
                .releaseName(releaseDescriptor.getReleaseName())
                .totalRemediation(metric == null ? 0 : metric.totalRemediations())
                .appliedRemediation(metric == null ? 0 : metric.appliedRemediations())
                .skippedRemediation(metric == null ? 0 : metric.skippedRemediations())
                .modifiedFiles(metric == null ? Set.of() : metric.modifiedFiles())
                .action(RemediationsApplyHelper.actionLabel(metric))
                .build()
                .toJson();
    }

    /**
     * Aggregates metrics and builds the cache apply result JSON.
     * Always-present fields match online table columns; cache-only fields are appended.
     */
    public static ObjectNode buildCacheResultNode(
            Path cacheZip, ApplyResult applyResult, Set<String> issueIdFilter) {
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                issueIdFilter, applyResult.metrics());
        List<String> releaseIds = applyResult.processedIds();
        String releaseId = releaseIds != null && !releaseIds.isEmpty() ? releaseIds.get(0) : null;
        return CacheResultData.builder()
                .common(CommonApplyResult.builder()
                        .releaseId(releaseId)
                        .applicationName(null)
                        .releaseName(null)
                        .totalRemediation(aggregated.totalRemediations())
                        .appliedRemediation(aggregated.appliedRemediations())
                        .skippedRemediation(aggregated.skippedRemediations())
                        .modifiedFiles(aggregated.modifiedFiles())
                        .action(RemediationsApplyHelper.actionLabel(aggregated))
                        .build())
                .cacheZip(cacheZip)
                .entryPaths(applyResult.processedEntries())
                .releaseIds(releaseIds)
                .build()
                .toJson();
    }

    /**
     * Properties always written for FoD apply-remediations, independent of online vs cache.
     * Keeps table columns stable across modes.
     */
    @Builder
    private record CommonApplyResult(
            String releaseId,
            String applicationName,
            String releaseName,
            int totalRemediation,
            int appliedRemediation,
            int skippedRemediation,
            Set<String> modifiedFiles,
            String action) {
        ObjectNode toJson() {
            ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
            result.put("releaseId", na(releaseId));
            result.put("applicationName", na(applicationName));
            result.put("releaseName", na(releaseName));
            result.put("totalRemediation", totalRemediation);
            result.put("appliedRemediation", appliedRemediation);
            result.put("skippedRemediation", skippedRemediation);
            result.set("modifiedFiles", toArrayNode(modifiedFiles));
            result.put(IActionCommandResultSupplier.actionFieldName, action);
            return result;
        }
    }

    @Builder
    private record CacheResultData(
            CommonApplyResult common,
            Path cacheZip,
            List<String> entryPaths,
            List<String> releaseIds) {
        ObjectNode toJson() {
            ObjectNode result = common.toJson();
            result.put("file", cacheZip.toString());
            result.set("entries", toStringArrayNode(entryPaths));
            result.set("releaseIds", toStringArrayNode(releaseIds));
            return result;
        }
    }

    private static String na(String value) {
        return value != null ? value : "N/A";
    }

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
