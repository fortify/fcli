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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsApplyHelper.ApplyResult;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

/**
 * FoD apply-remediations result JSON: product identity fields only;
 * metrics/action/cache extras come from {@link AviatorRemediationMetricsHelper}.
 */
public final class AviatorFoDApplyRemediationsHelper {
    private AviatorFoDApplyRemediationsHelper() {}

    public static ObjectNode buildOnlineResultNode(FoDReleaseDescriptor releaseDescriptor, ApplyResult applyResult) {
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                null, applyResult.metrics());
        return buildCommonNode(
                releaseDescriptor.getReleaseId(),
                releaseDescriptor.getApplicationName(),
                releaseDescriptor.getReleaseName(),
                aggregated);
    }

    public static ObjectNode buildCacheResultNode(
            Path cacheZip, ApplyResult applyResult, Set<String> issueIdFilter) {
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                issueIdFilter, applyResult.metrics());
        List<String> releaseIds = applyResult.processedIds();
        String releaseId = releaseIds != null && !releaseIds.isEmpty() ? releaseIds.get(0) : null;
        ObjectNode result = buildCommonNode(releaseId, null, null, aggregated);
        AviatorRemediationMetricsHelper.putCacheExtras(
                result, cacheZip, applyResult.processedEntries(), "releaseIds", releaseIds);
        return result;
    }

    private static ObjectNode buildCommonNode(
            String releaseId,
            String applicationName,
            String releaseName,
            RemediationMetric aggregated) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("releaseId", AviatorRemediationMetricsHelper.na(releaseId));
        result.put("applicationName", AviatorRemediationMetricsHelper.na(applicationName));
        result.put("releaseName", AviatorRemediationMetricsHelper.na(releaseName));
        AviatorRemediationMetricsHelper.putMetricAndAction(result, aggregated);
        return result;
    }
}
