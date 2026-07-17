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
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;

/**
 * Helper for Aviator SSC apply-remediations result JSON construction.
 */
public final class AviatorSSCApplyRemediationsHelper {
    private AviatorSSCApplyRemediationsHelper() {}

    /**
     * Builds the unified JSON result node for a single-artifact remediation (--artifact-id or --latest).
     * Uses the same output shape as buildAggregatedResultNode for consistent table columns.
     */
    public static ObjectNode buildResultNode(SSCArtifactDescriptor ad, int totalRemediation, int appliedRemediation,
            int skippedRemediation, Set<String> modifiedFiles, String action) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("appVersionId", ad.asObjectNode().path("projectVersionId").asText("N/A"));
        result.put("artifactId", ad.getId());
        result.put("artifactsProcessed", 1);
        result.put("artifactsSkipped", 0);
        result.put("totalRemediation", totalRemediation);
        result.put("appliedRemediation", appliedRemediation);
        result.put("skippedRemediation", skippedRemediation);
        result.set("modifiedFiles", toArrayNode(modifiedFiles));
        result.put(IActionCommandResultSupplier.actionFieldName, action);
        return result;
    }

    /**
     * Builds the unified JSON result node for --all, aggregating across all artifacts.
     * Uses the same output shape as buildResultNode for consistent table columns.
     */
    public static ObjectNode buildAggregatedResultNode(String appVersionId, int artifactsProcessed, int artifactsSkipped,
            int totalRemediation, int appliedRemediation, int skippedRemediation, Set<String> modifiedFiles, String action) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("appVersionId", appVersionId);
        result.put("artifactId", "N/A");
        result.put("artifactsProcessed", artifactsProcessed);
        result.put("artifactsSkipped", artifactsSkipped);
        result.put("totalRemediation", totalRemediation);
        result.put("appliedRemediation", appliedRemediation);
        result.put("skippedRemediation", skippedRemediation);
        result.set("modifiedFiles", toArrayNode(modifiedFiles));
        result.put(IActionCommandResultSupplier.actionFieldName, action);
        return result;
    }

    /**
     * Result shape for --from-cache: durable cache zip path and zip-relative entry paths only
     * (never ephemeral extract-dir absolute paths).
     */
    public static ObjectNode buildCacheResultNode(CacheResultData resultData) {
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

    public record CacheResultData(
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
