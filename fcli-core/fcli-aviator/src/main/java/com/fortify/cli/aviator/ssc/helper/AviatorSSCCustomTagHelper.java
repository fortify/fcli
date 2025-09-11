package com.fortify.cli.aviator.ssc.helper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCTagDefs.TagDefinition;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AviatorSSCCustomTagHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCCustomTagHelper.class);
    private final UnirestInstance unirest;
    private final TagDefinition tagDef;

    public JsonNode synchronize(AviatorSSCPrepareHelper.PrepareResult result) {
        try {
            LOG.debug("Searching for custom tag '{}' (GUID: {})", tagDef.getName(), tagDef.getGuid());
            ArrayNode customTags = (ArrayNode) unirest.get(SSCUrls.CUSTOM_TAGS).asObject(JsonNode.class).getBody().get("data");
            JsonNode existingTag = JsonHelper.stream(customTags)
                    .filter(tag -> tagDef.getGuid().equals(tag.get("guid").asText()))
                    .findFirst().orElse(null);

            if (existingTag != null) {
                return verifyAndUpdateExistingTag(result, existingTag);
            } else {
                return createNewTag(result);
            }
        } catch (UnexpectedHttpResponseException e) {
            LOG.error("Error synchronizing custom tag '{}': {}", tagDef.getName(), e.getMessage());
            result.addEntry("Custom Tag", "FAILED", "Error synchronizing tag '" + tagDef.getName() + "': " + e.getMessage());
            return null;
        }
    }

    private JsonNode verifyAndUpdateExistingTag(AviatorSSCPrepareHelper.PrepareResult result, JsonNode existingTag) {
        LOG.debug("Found existing tag '{}'. Verifying values.", tagDef.getName());
        JsonNode fullTagDetails = unirest.get(SSCUrls.CUSTOM_TAG(existingTag.get("id").asText())).asObject(JsonNode.class).getBody().get("data");
        Set<String> existingValues = JsonHelper.stream((ArrayNode) fullTagDetails.get("valueList")).map(v -> v.get("lookupValue").asText()).collect(Collectors.toSet());
        List<String> missingValues = tagDef.getValues().stream().filter(v -> !existingValues.contains(v)).collect(Collectors.toList());

        if (missingValues.isEmpty()) {
            LOG.info("Custom tag '{}' is already configured correctly.", tagDef.getName());
            result.addEntry("Custom Tag", "VERIFIED", "'" + tagDef.getName() + "' is already configured correctly.");
            return fullTagDetails;
        }

        LOG.info("Custom tag '{}' is missing values: {}", tagDef.getName(), missingValues);

        LOG.info("Updating custom tag '{}' to add missing values.", tagDef.getName());
        ObjectNode updatedTagBody = fullTagDetails.deepCopy();
        ArrayNode valuesArray = updatedTagBody.withArray("valueList");
        missingValues.forEach(v -> valuesArray.add(JsonHelper.getObjectMapper().createObjectNode().put("lookupValue", v)));
        LOG.debug("Update payload for tag '{}': {}", tagDef.getName(), updatedTagBody.toPrettyString());

        JsonNode updatedTag = unirest.put(SSCUrls.CUSTOM_TAG(existingTag.get("id").asText())).body(updatedTagBody).asObject(JsonNode.class).getBody().get("data");
        result.addEntry("Custom Tag", "UPDATED", "Added " + missingValues.size() + " missing values to tag '" + tagDef.getName() + "'.");
        return updatedTag;
    }

    private JsonNode createNewTag(AviatorSSCPrepareHelper.PrepareResult result) {
        LOG.info("Custom tag '{}' not found.", tagDef.getName());

        LOG.info("Creating custom tag '{}'...", tagDef.getName());
        JsonNode createPayload = getTagDefinitionForCreate();
        LOG.debug("Create payload for tag '{}': {}", tagDef.getName(), createPayload.toPrettyString());

        JsonNode createdTag = unirest.post(SSCUrls.CUSTOM_TAGS).body(createPayload).asObject(JsonNode.class).getBody().get("data");
        result.addEntry("Custom Tag", "CREATED", "Tag '" + tagDef.getName() + "' created successfully.");
        return createdTag;
    }

    private JsonNode getTagDefinitionForCreate() {
        ObjectNode tagNode = JsonHelper.getObjectMapper().createObjectNode();
        tagNode.put("name", tagDef.getName());
        tagNode.put("guid", tagDef.getGuid());
        tagNode.put("description", "Custom tag for Fortify Aviator.");
        tagNode.put("valueType", "LIST");
        tagNode.put("customTagType", "CUSTOM");
        ArrayNode values = tagNode.putArray("valueList");
        for (int i = 0; i < tagDef.getValues().size(); i++) {
            values.add(JsonHelper.getObjectMapper().createObjectNode()
                    .put("lookupValue", tagDef.getValues().get(i))
                    .put("deletable", true).put("hidden", false).put("seqNumber", i));
        }
        return tagNode;
    }
}