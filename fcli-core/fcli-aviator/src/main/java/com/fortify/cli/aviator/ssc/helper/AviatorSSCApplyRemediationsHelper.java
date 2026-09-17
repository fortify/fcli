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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationMetric;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;

/**
 * Helper class for the AviatorSSCAuditCommand to encapsulate
 * result message formatting and JSON output construction.
 */
public final class AviatorSSCApplyRemediationsHelper {
    private AviatorSSCApplyRemediationsHelper() {}

    /**
     * Builds the unified JSON result node for a single-artifact remediation (--artifact-id or --latest).
     * Uses the same output shape as buildAggregatedResultNode for consistent table columns.
     * @param ad The SSCArtifactDescriptor; its projectVersionId is used as appVersionId.
     * @param metric The remediation metric for this artifact.
     * @param action Final action.
     * @return An ObjectNode representing the result.
     */
    public static ObjectNode buildResultNode(SSCArtifactDescriptor ad, RemediationMetric metric, String action) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("appVersionId", ad.asObjectNode().path("projectVersionId").asText("N/A"));
        result.put("artifactId", ad.getId());
        result.put("artifactsProcessed", 1);
        result.put("artifactsSkipped", 0);
        result.put("totalRemediation", metric.totalRemediations());
        result.put("appliedRemediation", metric.appliedRemediations());
        result.put("identicalRemediation", metric.identicalRemediations());
        result.put("supersededRemediation", metric.supersededRemediations());
        result.put("possiblyRemediatedRemediation", metric.possiblyRemediatedRemediations());
        result.put("skippedRemediation", metric.skippedRemediations());
        result.put("skippedReasons", formatSkippedReasons(metric.skippedByReason()));
        result.set("skippedByReason", toObjectNode(metric.skippedByReason()));
        result.set("modifiedFiles", toArrayNode(metric.modifiedFiles()));
        result.put(IActionCommandResultSupplier.actionFieldName, action);
        return result;
    }

    /**
     * Builds the unified JSON result node for --all-open-issues, aggregating across all artifacts.
     * Uses the same output shape as buildResultNode for consistent table columns.
     * @param appVersionId The application version ID processed.
     * @param artifactsProcessed Number of artifacts successfully processed.
     * @param artifactsSkipped Number of artifacts skipped (e.g. no remediations.xml).
     * @param totalRemediation Total remediations across all artifacts.
     * @param appliedRemediation Total applied remediations across all artifacts.
     * @param skippedRemediation Total skipped remediations across all artifacts.
     * @param action Final action result.
     * @return An ObjectNode representing the aggregated result.
     */
    public static ObjectNode buildAggregatedResultNode(String appVersionId, int artifactsProcessed, int artifactsSkipped,
            RemediationMetric aggregatedMetric, String action) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("appVersionId", appVersionId);
        result.put("artifactId", "N/A");
        result.put("artifactsProcessed", artifactsProcessed);
        result.put("artifactsSkipped", artifactsSkipped);
        result.put("totalRemediation", aggregatedMetric.totalRemediations());
        result.put("appliedRemediation", aggregatedMetric.appliedRemediations());
        result.put("identicalRemediation", aggregatedMetric.identicalRemediations());
        result.put("supersededRemediation", aggregatedMetric.supersededRemediations());
        result.put("possiblyRemediatedRemediation", aggregatedMetric.possiblyRemediatedRemediations());
        result.put("skippedRemediation", aggregatedMetric.skippedRemediations());
        result.put("skippedReasons", formatSkippedReasons(aggregatedMetric.skippedByReason()));
        result.set("skippedByReason", toObjectNode(aggregatedMetric.skippedByReason()));
        result.set("modifiedFiles", toArrayNode(aggregatedMetric.modifiedFiles()));
        result.put(IActionCommandResultSupplier.actionFieldName, action);
        return result;
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
}
