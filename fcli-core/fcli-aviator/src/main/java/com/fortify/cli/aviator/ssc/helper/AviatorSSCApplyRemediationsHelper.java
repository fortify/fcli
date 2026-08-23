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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper.ApplyResult;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;

/**
 * SSC apply-remediations result JSON: product identity fields only;
 * metrics/action/cache extras come from {@link AviatorRemediationMetricsHelper}.
 */
public final class AviatorSSCApplyRemediationsHelper {
    private AviatorSSCApplyRemediationsHelper() {}

    public static ObjectNode buildOnlineResultNode(
            List<SSCArtifactDescriptor> artifacts,
            String appVersionId,
            ApplyResult applyResult,
            Set<String> issueIdFilter) {
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                issueIdFilter, applyResult.metrics());
        return buildCommonNode(
                resolveAppVersionId(artifacts, appVersionId),
                resolveSingleArtifactId(artifacts, applyResult),
                applyResult,
                aggregated);
    }

    public static ObjectNode buildCacheResultNode(
            Path cacheZip,
            ApplyResult applyResult,
            Set<String> issueIdFilter,
            Map<String, String> selection) {
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                issueIdFilter, applyResult.metrics());
        String appVersionId = selection != null ? selection.get("appVersionId") : null;
        ObjectNode result = buildCommonNode(appVersionId, null, applyResult, aggregated);
        AviatorRemediationMetricsHelper.putCacheExtras(
                result, cacheZip, applyResult.processedEntries(), "artifactIds", applyResult.processedIds());
        return result;
    }

    private static ObjectNode buildCommonNode(
            String appVersionId,
            String artifactId,
            ApplyResult applyResult,
            RemediationMetric aggregated) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("appVersionId", AviatorRemediationMetricsHelper.na(appVersionId));
        result.put("artifactId", AviatorRemediationMetricsHelper.na(artifactId));
        result.put("artifactsProcessed", applyResult.metrics().size());
        result.put("artifactsSkipped", applyResult.skipped());
        result.put("previewMode", aggregated instanceof RemediationMetric.Preview);
        AviatorRemediationMetricsHelper.putMetricAndAction(result, aggregated);
        return result;
    }

    private static String resolveAppVersionId(List<SSCArtifactDescriptor> artifacts, String appVersionId) {
        if (appVersionId != null || artifacts == null || artifacts.size() != 1) {
            return appVersionId;
        }
        return artifacts.get(0).asObjectNode().path("projectVersionId").asText(null);
    }

    private static String resolveSingleArtifactId(List<SSCArtifactDescriptor> artifacts, ApplyResult applyResult) {
        if (artifacts == null || artifacts.size() != 1) {
            return null;
        }
        if (applyResult.metrics().size() == 1 && applyResult.skipped() == 0) {
            return artifacts.get(0).getId();
        }
        return null;
    }
}
