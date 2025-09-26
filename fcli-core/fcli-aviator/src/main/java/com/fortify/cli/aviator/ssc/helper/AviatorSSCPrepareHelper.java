package com.fortify.cli.aviator.ssc.helper;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

import kong.unirest.UnirestInstance;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import com.fortify.cli.common.json.JsonHelper;

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

    @Data @RequiredArgsConstructor
    private static class ResultEntry {
        private final String status;
        private final String entity;
        private final String details;
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