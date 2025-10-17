/*
 * Copyright 2021-2025 Open Text.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AviatorSSCPrepareHelper {
    private final UnirestInstance unirest;

    @Builder @Data
    public static class PrepareOptions {
        private String issueTemplateNameOrId;
        private boolean allIssueTemplates;
        private String appVersionNameOrId;
        private boolean allAppVersions;
    }

    @Data
    public static class PrepareResult {
        private final List<ResultEntry> entries = new ArrayList<>();
        public void addEntry(String entity, String status, String details) { entries.add(new ResultEntry(status, entity, details)); }
        public JsonNode toJsonNode() { return JsonHelper.getObjectMapper().valueToTree(entries); }
    }

    @Data @Reflectable @NoArgsConstructor @AllArgsConstructor
    private static class ResultEntry {
        private String status;
        private String entity;
        private String details;
    }

    @Data
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
            var tagHelperStatus = new AviatorSSCCustomTagHelper(unirest, AviatorSSCTagDefs.AVIATOR_STATUS_TAG);

            JsonNode predictionTag = tagHelperPrediction.synchronize(result);
            JsonNode statusTag = tagHelperStatus.synchronize(result);

            if (predictionTag == null || statusTag == null) {
                result.addEntry("Global", "HALTED", "Failed to synchronize one or more required Aviator custom tags.");
                return result;
            }
            List<JsonNode> requiredTags = List.of(predictionTag, statusTag);

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
