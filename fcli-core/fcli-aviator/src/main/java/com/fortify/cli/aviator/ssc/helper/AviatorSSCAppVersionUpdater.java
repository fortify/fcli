package com.fortify.cli.aviator.ssc.helper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AviatorSSCAppVersionUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCAppVersionUpdater.class);
    private final UnirestInstance unirest;

    public void process(AviatorSSCPrepareHelper.PrepareOptions options, AviatorSSCPrepareHelper.PrepareResult result, List<JsonNode> aviatorTags, IProgressWriter progress) {
        progress.writeProgress("Discovering application versions...");
        Set<String> requiredTagGuids = aviatorTags.stream().map(t -> t.get("guid").asText()).collect(Collectors.toSet());
        List<SSCAppVersionDescriptor> targetAvs = getTargetAppVersions(options);
        if (targetAvs.isEmpty()) {
            LOG.info("No target application versions found to process.");
            return;
        }
        LOG.debug("Found {} target application versions.", targetAvs.size());

        Map<String, ArrayNode> currentTagsByAvId = fetchCurrentTagsForAvs(targetAvs);
        AviatorSSCPrepareHelper.ResultCounter counter = new AviatorSSCPrepareHelper.ResultCounter();

        List<SSCAppVersionDescriptor> versionsToUpdate = targetAvs.stream().filter(av -> {
            ArrayNode currentTags = currentTagsByAvId.get(av.getVersionId());
            Set<String> existingGuids = JsonHelper.stream(currentTags).map(tag -> tag.get("guid").asText()).collect(Collectors.toSet());
            boolean hasAllTags = existingGuids.containsAll(requiredTagGuids);
            if (hasAllTags) {
                counter.incrementSkipped();
                LOG.debug("Application version '{}' already has all required tags.", av.getApplicationName() + ":" + av.getVersionName());
            }
            return !hasAllTags;
        }).collect(Collectors.toList());

        if (versionsToUpdate.isEmpty()) {
            LOG.info("All targeted application versions are already configured.");
            result.addEntry("Application Versions", "SKIPPED", "All " + counter.getSkipped() + " targeted version(s) were already configured.");
            return;
        }

        LOG.info("{} application versions require updates.", versionsToUpdate.size());

        progress.writeProgress("Updating %d application versions...", versionsToUpdate.size());
        SSCBulkRequestBuilder updateRequest = new SSCBulkRequestBuilder();
        versionsToUpdate.forEach(av -> {
            ArrayNode updatedTags = currentTagsByAvId.get(av.getVersionId()).deepCopy();
            Set<String> existingGuids = JsonHelper.stream(updatedTags).map(tag -> tag.get("guid").asText()).collect(Collectors.toSet());
            requiredTagGuids.forEach(guid -> {
                if (!existingGuids.contains(guid)) {
                    updatedTags.add(JsonHelper.getObjectMapper().createObjectNode().put("guid", guid));
                }
            });
            updateRequest.request(av.getVersionId(), unirest.put(SSCUrls.PROJECT_VERSION_CUSTOM_TAGS(av.getVersionId())).body(updatedTags));
        });

        LOG.debug("Sending bulk update request for application versions: {}", updateRequest);
        handleBulkUpdate(updateRequest, versionsToUpdate, SSCAppVersionDescriptor::getVersionId, av -> av.getApplicationName() + ":" + av.getVersionName(), result, counter, "application version");
        addSummaryEntry(result, "Application Versions", counter);
    }

    private List<SSCAppVersionDescriptor> getTargetAppVersions(AviatorSSCPrepareHelper.PrepareOptions options) {
        GetRequest request = unirest.get(SSCUrls.PROJECT_VERSIONS).queryString("limit", "-1").queryString("fields", "id,name,project");
        if (!options.isAllAppVersions()) {
            request.queryString("q", String.format("id:%d", SSCAppVersionHelper.getRequiredAppVersion(unirest, options.getAppVersionNameOrId(), ":").getIntVersionId()));
        }
        ArrayNode avsNode = (ArrayNode) request.asObject(JsonNode.class).getBody().get("data");
        return JsonHelper.stream(avsNode).map(n -> JsonHelper.treeToValue(n, SSCAppVersionDescriptor.class)).collect(Collectors.toList());
    }

    private Map<String, ArrayNode> fetchCurrentTagsForAvs(List<SSCAppVersionDescriptor> avs) {
        LOG.info("Fetching associated custom tags for {} application versions...", avs.size());
        SSCBulkRequestBuilder readRequest = new SSCBulkRequestBuilder();
        avs.forEach(av -> readRequest.request(av.getVersionId(), unirest.get(SSCUrls.PROJECT_VERSION_CUSTOM_TAGS(av.getVersionId()))));
        var readResponse = readRequest.execute(unirest);
        return avs.stream().collect(Collectors.toMap(SSCAppVersionDescriptor::getVersionId, av -> (ArrayNode) readResponse.data(av.getVersionId())));
    }

    private <T> void handleBulkUpdate(SSCBulkRequestBuilder request, List<T> items, Function<T, String> idExtractor, Function<T, String> nameExtractor, AviatorSSCPrepareHelper.PrepareResult result, AviatorSSCPrepareHelper.ResultCounter counter, String entityType) {
        try {
            var response = request.execute(unirest);
            items.forEach(item -> {
                String id = idExtractor.apply(item);
                JsonNode responseBody = response.body(id);
                if (responseBody.get("responseCode").asInt() < 300) {
                    counter.incrementSucceeded();
                    LOG.debug("Successfully updated {} '{}'", entityType, nameExtractor.apply(item));
                } else {
                    counter.incrementFailed();
                    String failureMsg = "Failed to update " + entityType + " '" + nameExtractor.apply(item) + "': " + responseBody;
                    LOG.warn(failureMsg);
                    result.addEntry(entityType + ": " + nameExtractor.apply(item), "FAILED", responseBody.toString());
                }
            });
        } catch (Exception e) {
            items.forEach(item -> counter.incrementFailed());
            LOG.error("Bulk update for {}s failed.", entityType, e);
            result.addEntry("Bulk " + entityType + "s", "FAILED", e.getMessage());
        }
    }

    private void addSummaryEntry(AviatorSSCPrepareHelper.PrepareResult result, String entityName, AviatorSSCPrepareHelper.ResultCounter counter) {
        String status = counter.getFailed() > 0 ? "FAILED" : "SUCCEEDED";
        String details = String.format("Succeeded: %d, Failed: %d, Skipped: %d", counter.getSucceeded(), counter.getFailed(), counter.getSkipped());
        if (counter.getFailed() > 0) {
            details += ". See logs or run with -o json for individual failure details.";
        }
        result.addEntry(entityName, status, details);
    }
}