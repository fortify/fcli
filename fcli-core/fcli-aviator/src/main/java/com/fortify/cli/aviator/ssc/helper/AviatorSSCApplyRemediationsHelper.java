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
 */
public final class AviatorSSCApplyRemediationsHelper {
    private AviatorSSCApplyRemediationsHelper() {}

    /**
     * Builds a single online-result shape for any apply outcome (one or many artifacts).
     * Table columns use the same fields for --artifact-id, --latest, and --all.
     */
    public static ObjectNode buildOnlineResultNode(
            List<SSCArtifactDescriptor> artifacts,
            String appVersionId,
            ApplyResult applyResult,
            Set<String> issueIdFilter) {
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                issueIdFilter, applyResult.metrics());

        String versionId = appVersionId;
        String artifactId = "N/A";
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

        return toOnlineJson(OnlineResultData.builder()
                .appVersionId(versionId != null ? versionId : "N/A")
                .artifactId(artifactId)
                .artifactsProcessed(applyResult.metrics().size())
                .artifactsSkipped(applyResult.skipped())
                .totalRemediation(aggregated.totalRemediations())
                .appliedRemediation(aggregated.appliedRemediations())
                .skippedRemediation(aggregated.skippedRemediations())
                .modifiedFiles(aggregated.modifiedFiles())
                .action(RemediationsApplyHelper.actionLabel(aggregated))
                .build());
    }

    /**
     * Aggregates metrics and builds the cache apply result JSON (same fields as before).
     *
     * @param selection manifest selection map; may be null (appVersionId becomes null / "N/A")
     */
    public static ObjectNode buildCacheResultNode(
            Path cacheZip,
            ApplyResult applyResult,
            Set<String> issueIdFilter,
            Map<String, String> selection) {
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                issueIdFilter, applyResult.metrics());
        String appVersionId = selection != null ? selection.get("appVersionId") : null;
        return toCacheJson(CacheResultData.builder()
                .cacheZip(cacheZip)
                .entryPaths(applyResult.processedEntries())
                .artifactIds(applyResult.processedIds())
                .appVersionId(appVersionId)
                .fprsProcessed(applyResult.metrics().size())
                .fprsSkipped(applyResult.skipped())
                .totalRemediation(aggregated.totalRemediations())
                .appliedRemediation(aggregated.appliedRemediations())
                .skippedRemediation(aggregated.skippedRemediations())
                .modifiedFiles(aggregated.modifiedFiles())
                .action(RemediationsApplyHelper.actionLabel(aggregated))
                .build());
    }

    private static ObjectNode toOnlineJson(OnlineResultData data) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("appVersionId", data.appVersionId() != null ? data.appVersionId() : "N/A");
        result.put("artifactId", data.artifactId() != null ? data.artifactId() : "N/A");
        result.put("artifactsProcessed", data.artifactsProcessed());
        result.put("artifactsSkipped", data.artifactsSkipped());
        result.put("totalRemediation", data.totalRemediation());
        result.put("appliedRemediation", data.appliedRemediation());
        result.put("skippedRemediation", data.skippedRemediation());
        result.set("modifiedFiles", toArrayNode(data.modifiedFiles()));
        result.put(IActionCommandResultSupplier.actionFieldName, data.action());
        return result;
    }

    private static ObjectNode toCacheJson(CacheResultData resultData) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("appVersionId", resultData.appVersionId() != null ? resultData.appVersionId() : "N/A");
        result.put("artifactId", "N/A");
        result.put("file", resultData.cacheZip().toString());
        result.put("artifactsProcessed", resultData.fprsProcessed());
        result.put("artifactsSkipped", resultData.fprsSkipped());
        result.put("totalRemediation", resultData.totalRemediation());
        result.put("appliedRemediation", resultData.appliedRemediation());
        result.put("skippedRemediation", resultData.skippedRemediation());
        result.set("modifiedFiles", toArrayNode(resultData.modifiedFiles()));
        result.set("entries", toStringArrayNode(resultData.entryPaths()));
        result.set("artifactIds", toStringArrayNode(resultData.artifactIds()));
        result.put(IActionCommandResultSupplier.actionFieldName, resultData.action());
        return result;
    }

    @Builder
    private record OnlineResultData(
            String appVersionId,
            String artifactId,
            int artifactsProcessed,
            int artifactsSkipped,
            int totalRemediation,
            int appliedRemediation,
            int skippedRemediation,
            Set<String> modifiedFiles,
            String action) {}

    @Builder
    private record CacheResultData(
            Path cacheZip,
            List<String> entryPaths,
            List<String> artifactIds,
            String appVersionId,
            int fprsProcessed,
            int fprsSkipped,
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
