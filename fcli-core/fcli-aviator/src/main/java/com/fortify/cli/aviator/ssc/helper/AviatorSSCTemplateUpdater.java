package com.fortify.cli.aviator.ssc.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateDescriptor;
import com.fortify.cli.ssc.issue_template.helper.SSCIssueTemplateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.bulk.SSCBulkRequestBuilder;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AviatorSSCTemplateUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCTemplateUpdater.class);
    private final UnirestInstance unirest;

    public void process(AviatorSSCPrepareHelper.PrepareOptions options, AviatorSSCPrepareHelper.PrepareResult result, List<JsonNode> aviatorTags, IProgressWriter progress) {
        progress.writeProgress("Discovering issue templates...");
        Set<Integer> requiredTagIds = aviatorTags.stream().map(t -> t.get("id").asInt()).collect(Collectors.toSet());
        if (requiredTagIds.contains(-1)) {
            result.addEntry("Issue Templates", "SKIPPED", "Processing skipped as one or more custom tags do not exist.");
            return;
        }

        Collection<SSCIssueTemplateDescriptor> targetTemplates = getTargetTemplates(options);
        LOG.debug("Found {} target issue templates.", targetTemplates.size());
        Map<String, ArrayNode> currentTagsByTemplateId = fetchCurrentTagsForTemplates(targetTemplates);
        AviatorSSCPrepareHelper.ResultCounter counter = new AviatorSSCPrepareHelper.ResultCounter();

        List<SSCIssueTemplateDescriptor> templatesToUpdate = targetTemplates.stream().filter(t -> {
            ArrayNode currentTags = currentTagsByTemplateId.get(t.getId());
            Set<Integer> existingTagIds = JsonHelper.stream(currentTags).map(tag -> tag.get("id").asInt()).collect(Collectors.toSet());
            boolean hasAllTags = existingTagIds.containsAll(requiredTagIds);
            if (hasAllTags) counter.incrementSkipped();
            return !hasAllTags;
        }).collect(Collectors.toList());

        if (templatesToUpdate.isEmpty()) {
            LOG.info("All targeted issue templates are already configured.");
            result.addEntry("Issue Templates", "SKIPPED", "All " + counter.getSkipped() + " targeted template(s) were already configured.");
            return;
        }

        LOG.info("{} issue templates require updates.", templatesToUpdate.size());

        progress.writeProgress("Updating %d issue templates...", templatesToUpdate.size());
        SSCBulkRequestBuilder bulkRequest = new SSCBulkRequestBuilder();
        templatesToUpdate.forEach(template -> {
            ArrayNode updatedTagsPayload = JsonHelper.getObjectMapper().createArrayNode();

            // Get the set of all unique tag IDs (existing + required)
            Set<Integer> allTagIds = JsonHelper.stream(currentTagsByTemplateId.get(template.getId()))
                    .map(tag -> tag.get("id").asInt())
                    .collect(Collectors.toSet());
            allTagIds.addAll(requiredTagIds);

            // Build the final payload as an array of objects, each with an "id" property
            allTagIds.forEach(id -> updatedTagsPayload.add(JsonHelper.getObjectMapper().createObjectNode().put("id", id)));

            bulkRequest.request(template.getId(), unirest.put(SSCUrls.ISSUE_TEMPLATE_CUSTOM_TAGS(template.getId())).body(updatedTagsPayload));
        });

        LOG.debug("Sending bulk update request for issue templates: {}", bulkRequest.toString());
        handleBulkUpdate(bulkRequest, templatesToUpdate, SSCIssueTemplateDescriptor::getId, SSCIssueTemplateDescriptor::getName, result, counter, "template");
        addSummaryEntry(result, "Issue Templates", counter);
    }

    private Map<String, ArrayNode> fetchCurrentTagsForTemplates(Collection<SSCIssueTemplateDescriptor> templates) {
        LOG.info("Fetching associated custom tags for {} issue templates...", templates.size());
        SSCBulkRequestBuilder readRequest = new SSCBulkRequestBuilder();
        templates.forEach(t -> readRequest.request(t.getId(), unirest.get(SSCUrls.ISSUE_TEMPLATE_CUSTOM_TAGS(t.getId()))));
        var readResponse = readRequest.execute(unirest);
        return templates.stream().collect(Collectors.toMap(SSCIssueTemplateDescriptor::getId, t -> (ArrayNode) readResponse.data(t.getId())));
    }

    private Collection<SSCIssueTemplateDescriptor> getTargetTemplates(AviatorSSCPrepareHelper.PrepareOptions options) {
        SSCIssueTemplateHelper helper = new SSCIssueTemplateHelper(unirest);
        if (options.isAllIssueTemplates()) {
            return new ArrayList<>(helper.getDescriptorsById().values());
        } else {
            return List.of(helper.getDescriptorByNameOrId(options.getIssueTemplateNameOrId(), true));
        }
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
                    String failureMsg = "Failed to update " + entityType + " '" + nameExtractor.apply(item) + "': " + responseBody.toString();
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