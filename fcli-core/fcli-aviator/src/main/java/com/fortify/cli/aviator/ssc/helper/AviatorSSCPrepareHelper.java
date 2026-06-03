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
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCustomTagHelper.SynchronizationResult;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

import kong.unirest.UnirestInstance;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AviatorSSCPrepareHelper {
    private final UnirestInstance unirest;

    @Builder @Data @Reflectable
    public static class PrepareOptions {
        private String issueTemplateNameOrId;
        private boolean allIssueTemplates;
        private String appVersionNameOrId;
        private boolean allAppVersions;
    }

    @Data @Reflectable
    public static class PrepareResult {
        private final List<ResultEntry> entries = new ArrayList<>();
        public void addEntry(String entity, String status, String details) { entries.add(new ResultEntry(status, entity, details)); }
        public JsonNode toJsonNode() { return JsonHelper.getObjectMapper().valueToTree(entries); }
    }

    @Data @RequiredArgsConstructor @Reflectable
    private static class ResultEntry {
        private final String status;
        private final String entity;
        private final String details;
    }

    @Data @Reflectable
    public static class ResultCounter {
        private int succeeded = 0, failed = 0, skipped = 0;
        public int getTotal() { return succeeded + failed + skipped; }
        public void incrementSucceeded() { this.succeeded++; }
        public void incrementFailed() { this.failed++; }
        public void incrementSkipped() { this.skipped++; }
    }

    public PrepareResult prepare(PrepareOptions options) {
        PrepareResult result = new PrepareResult();
        try (IProgressWriter progress = ProgressWriterType.auto.create()) {

            progress.writeProgress("Synchronizing Aviator custom tags...");
            var tagHelperPrediction = new AviatorSSCCustomTagHelper(unirest, AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG);
            var tagHelperStatus     = new AviatorSSCCustomTagHelper(unirest, AviatorSSCTagDefs.AVIATOR_STATUS_TAG);
            var tagHelperDastCorr   = new AviatorSSCCustomTagHelper(unirest, AviatorSSCTagDefs.DAST_CORRELATION_STATUS_TAG);

            SynchronizationResult predictionResult = tagHelperPrediction.synchronize(result);
            SynchronizationResult statusResult     = tagHelperStatus.synchronize(result);
            SynchronizationResult dastCorrResult   = tagHelperDastCorr.synchronize(result);

            // Required Aviator tags must succeed (either as custom or system-managed)
            if (!predictionResult.isSuccessful() || !statusResult.isSuccessful()) {
                result.addEntry("Global", "HALTED", "Failed to synchronize one or more required Aviator custom tags.");
                return result;
            }
            // DAST correlation tag is optional - just warn if it fails
            if (!dastCorrResult.isSuccessful()) {
                result.addEntry("DAST Correlation Tag", "WARNING",
                    "Failed to synchronize 'DAST correlation status' tag. SAST-DAST correlation feature may not be fully visible in SSC UI.");
            }

            // Always synchronize attributes (before any early return)
            progress.writeProgress("Synchronizing Aviator custom attributes...");
            new AviatorSSCCorrelationAttributeHelper(unirest, AviatorSSCCorrelationAttributeDefs.LAST_CORRELATION_ATTR)
                .synchronize(result);

            // Build list of tags requiring manual association (excludes system-managed tags)
            List<SynchronizationResult> allResults = List.of(predictionResult, statusResult, dastCorrResult);
            List<JsonNode> requiredTags = allResults.stream()
                    .filter(SynchronizationResult::requiresAssociation)
                    .map(SynchronizationResult::getTag)
                    .collect(Collectors.toList());

            // If all Aviator tags are system-managed (SSC 26.2+), skip template/version association
            if (requiredTags.isEmpty()) {
                result.addEntry("Global", "INFO",
                        "All Aviator tags are system-managed (SSC 26.2+). No manual template/version association required.");
                progress.writeInfo("All Aviator tags are system-managed by SSC. No manual association needed.");
                return result;
            }

            if (options.isAllIssueTemplates() || options.getIssueTemplateNameOrId() != null) {
                new AviatorSSCTemplateUpdater(unirest).process(options, result, requiredTags, progress);
            }

            if (options.isAllAppVersions() || options.getAppVersionNameOrId() != null) {
                new AviatorSSCAppVersionUpdater(unirest).process(options, result, requiredTags, progress);
            }
        }
        return result;
    }
}
