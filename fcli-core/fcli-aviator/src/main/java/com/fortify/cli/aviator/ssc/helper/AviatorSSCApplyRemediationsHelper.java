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
package com.fortify.cli.aviator.ssc.helper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper.ApplyResult;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;

import lombok.Builder;

/**
 * Helper for Aviator SSC apply-remediations result JSON construction.
 * <p>
 * Always-present fields live in {@link CommonApplyResult} (online = common only).
 * Cache mode adds mode-specific fields via {@link CacheResultData#toJson()}.
 */
public final class AviatorSSCApplyRemediationsHelper {
    private AviatorSSCApplyRemediationsHelper() {}

    /**
     * Builds online-result JSON (always-present fields only).
     * Table columns stay stable for --artifact-id, --latest, and --all.
     */
    public static ObjectNode buildOnlineResultNode(
            List<SSCArtifactDescriptor> artifacts,
            String appVersionId,
            ApplyResult applyResult,
            Set<String> issueIdFilter) {
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                issueIdFilter, applyResult.metrics());

        String versionId = appVersionId;
        String artifactId = null;
        if (artifacts.size() == 1) {
            SSCArtifactDescriptor only = artifacts.get(0);
            if (versionId == null) {
                versionId = only.asObjectNode().path("projectVersionId").asText(null);
            }
            // Preserve single-artifact id when that one entry was processed successfully.
            if (applyResult.metrics().size() == 1 && applyResult.skipped() == 0) {
                artifactId = only.getId();
            }
        }

        return commonFrom(versionId, artifactId, applyResult, aggregated).toJson();
    }

    /**
     * Aggregates metrics and builds the cache apply result JSON.
     * Always-present fields match online table columns; cache-only fields are appended.
     *
     * @param selection manifest selection map; may be null (appVersionId becomes "N/A")
     */
    public static ObjectNode buildCacheResultNode(
            Path cacheZip,
            ApplyResult applyResult,
            Set<String> issueIdFilter,
            Map<String, String> selection) {
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                issueIdFilter, applyResult.metrics());
        String appVersionId = selection != null ? selection.get("appVersionId") : null;
        return CacheResultData.builder()
                .common(commonFrom(appVersionId, null, applyResult, aggregated))
                .cacheZip(cacheZip)
                .entryPaths(applyResult.processedEntries())
                .artifactIds(applyResult.processedIds())
                .build()
                .toJson();
    }

    private static CommonApplyResult commonFrom(
            String appVersionId,
            String artifactId,
            ApplyResult applyResult,
            RemediationMetric aggregated) {
        return CommonApplyResult.builder()
                .appVersionId(appVersionId)
                .artifactId(artifactId)
                .artifactsProcessed(applyResult.metrics().size())
                .artifactsSkipped(applyResult.skipped())
                .totalRemediation(aggregated.totalRemediations())
                .appliedRemediation(aggregated.appliedRemediations())
                .skippedRemediation(aggregated.skippedRemediations())
                .skippedByReason(aggregated.skippedByReason())
                .modifiedFiles(aggregated.modifiedFiles())
                .action(RemediationsApplyHelper.actionLabel(aggregated))
                .build();
    }

    /**
     * Properties always written for SSC apply-remediations, independent of online vs cache.
     * Keeps table columns stable across modes.
     */
    @Builder
    private record CommonApplyResult(
            String appVersionId,
            String artifactId,
            int artifactsProcessed,
            int artifactsSkipped,
            int totalRemediation,
            int appliedRemediation,
            int skippedRemediation,
            Map<String, Integer> skippedByReason,
            Set<String> modifiedFiles,
            String action) {
        ObjectNode toJson() {
            ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
            result.put("appVersionId", na(appVersionId));
            result.put("artifactId", na(artifactId));
            result.put("artifactsProcessed", artifactsProcessed);
            result.put("artifactsSkipped", artifactsSkipped);
            result.put("totalRemediation", totalRemediation);
            result.put("appliedRemediation", appliedRemediation);
            result.put("skippedRemediation", skippedRemediation);
            result.put("skippedReasons", formatSkippedReasons(skippedByReason));
            result.set("skippedByReason", toObjectNode(skippedByReason));
            result.set("modifiedFiles", toArrayNode(modifiedFiles));
            result.put(IActionCommandResultSupplier.actionFieldName, action);
            return result;
        }
    }

    /** Cache mode: always-present fields plus cache path, entry paths, and artifact id list. */
    @Builder
    private record CacheResultData(
            CommonApplyResult common,
            Path cacheZip,
            List<String> entryPaths,
            List<String> artifactIds) {
        ObjectNode toJson() {
            ObjectNode result = common.toJson();
            result.put("file", cacheZip.toString());
            result.set("entries", toStringArrayNode(entryPaths));
            result.set("artifactIds", toStringArrayNode(artifactIds));
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

    private static ObjectNode toObjectNode(Map<String, Integer> skippedByReason) {
        ObjectNode object = JsonHelper.getObjectMapper().createObjectNode();
        if (skippedByReason != null) {
            skippedByReason.forEach(object::put);
        }
        return object;
    }

    private static String formatSkippedReasons(Map<String, Integer> skippedByReason) {
        if (skippedByReason == null || skippedByReason.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        skippedByReason.forEach((reason, count) -> parts.add(reason + "=" + count));
        return String.join(", ", parts);
    }

    private static ArrayNode toStringArrayNode(List<String> values) {
        ArrayNode array = JsonHelper.getObjectMapper().createArrayNode();
        if (values != null) {
            values.forEach(array::add);
        }
        return array;
    }
}
