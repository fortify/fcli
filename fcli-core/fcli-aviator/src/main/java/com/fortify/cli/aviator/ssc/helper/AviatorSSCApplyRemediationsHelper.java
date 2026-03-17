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

import com.fasterxml.jackson.databind.node.ObjectNode;
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
     * @param totalRemediation Total no. of remediations in the artifact.
     * @param appliedRemediation Remediations applied successfully.
     * @param skippedRemediation Remediations skipped.
     * @param action Final action.
     * @return An ObjectNode representing the result.
     */
    public static ObjectNode buildResultNode(SSCArtifactDescriptor ad, int totalRemediation, int appliedRemediation, int skippedRemediation, String action) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("appVersionId", ad.asObjectNode().path("projectVersionId").asText("N/A"));
        result.put("artifactId", ad.getId());
        result.put("artifactsProcessed", 1);
        result.put("artifactsSkipped", 0);
        result.put("totalRemediation", totalRemediation);
        result.put("appliedRemediation", appliedRemediation);
        result.put("skippedRemediation", skippedRemediation);
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
            int totalRemediation, int appliedRemediation, int skippedRemediation, String action) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("appVersionId", appVersionId);
        result.put("artifactId", "N/A");
        result.put("artifactsProcessed", artifactsProcessed);
        result.put("artifactsSkipped", artifactsSkipped);
        result.put("totalRemediation", totalRemediation);
        result.put("appliedRemediation", appliedRemediation);
        result.put("skippedRemediation", skippedRemediation);
        result.put(IActionCommandResultSupplier.actionFieldName, action);
        return result;
    }

}
