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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCTagDefs.TagDefinition;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AviatorSSCCustomTagHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCCustomTagHelper.class);
    private final UnirestInstance unirest;
    private final TagDefinition tagDef;

    /**
     * Result of tag synchronization, indicating whether the tag was found/created
     * and whether it's system-managed (SSC 26.2+ built-in) or requires manual association.
     */
    @Getter @RequiredArgsConstructor @Reflectable
    public static final class SynchronizationResult {
        private final JsonNode tag;
        private final boolean successful;
        private final boolean systemManaged;

        /**
         * Returns true if the tag was successfully synchronized and requires manual
         * association with issue templates/app versions. System-managed tags (SSC 26.2+
         * built-in Aviator tags) are automatically associated and don't need manual setup.
         */
        public boolean requiresAssociation() {
            return successful && !systemManaged;
        }

        public static SynchronizationResult success(JsonNode tag, boolean systemManaged) {
            return new SynchronizationResult(tag, true, systemManaged);
        }

        public static SynchronizationResult failure() {
            return new SynchronizationResult(null, false, false);
        }
    }

    /**
     * Synchronizes the custom tag with SSC. For Aviator built-in tags (prediction/status),
     * also checks the /internalCustomTags endpoint to detect SSC 26.2+ system-managed tags.
     *
     * @param result the prepare result to record status entries
     * @return SynchronizationResult indicating success/failure and whether system-managed
     */
    public SynchronizationResult synchronize(AviatorSSCPrepareHelper.PrepareResult result) {
        try {
            JsonNode existingTag = findExistingCustomTag();
            if (existingTag != null) {
                return handleExistingTag(existingTag, result);
            }

            SynchronizationResult systemManagedResult = checkSystemManagedTag(result);
            if (systemManagedResult != null) {
                return systemManagedResult;
            }

            return createTagOrFail(result);
        } catch (UnexpectedHttpResponseException e) {
            return handleSynchronizationError(e, result);
        }
    }

    /** Searches for an existing custom tag by GUID in /customTags endpoint. */
    private JsonNode findExistingCustomTag() {
        LOG.debug("Searching for custom tag '{}' (GUID: {})", tagDef.getName(), tagDef.getGuid());
        ArrayNode customTags = (ArrayNode) unirest.get(SSCUrls.CUSTOM_TAGS)
                .asObject(JsonNode.class).getBody().get("data");
        return JsonHelper.stream(customTags)
                .filter(tag -> tagDef.getGuid().equals(tag.get("guid").asText()))
                .findFirst().orElse(null);
    }

    /** Handles an existing custom tag by verifying/updating it. */
    private SynchronizationResult handleExistingTag(JsonNode existingTag,
            AviatorSSCPrepareHelper.PrepareResult result) {
        JsonNode updatedTag = verifyAndUpdateExistingTag(result, existingTag);
        return SynchronizationResult.success(updatedTag, false);
    }

    /**
     * Checks if this is a system-managed Aviator tag (SSC 26.2+).
     * Returns SynchronizationResult if found, null otherwise.
     */
    private SynchronizationResult checkSystemManagedTag(AviatorSSCPrepareHelper.PrepareResult result) {
        if (!isAviatorBuiltInTag()) {
            return null;
        }
        JsonNode internalTag = findInInternalCustomTags();
        if (internalTag == null) {
            return null;
        }
        LOG.info("Tag '{}' found as system-managed internal tag (SSC 26.2+). Configure through SSC.",
                tagDef.getName());
        result.addEntry("Custom Tag", "SYSTEM_MANAGED",
                "'" + tagDef.getName() + "' is a built-in SSC tag (SSC 26.2+). Configure through SSC.");
        return SynchronizationResult.success(internalTag, true);
    }

    /** Creates a new tag or returns failure if creation fails. */
    private SynchronizationResult createTagOrFail(AviatorSSCPrepareHelper.PrepareResult result) {
        JsonNode createdTag = createNewTag(result);
        return createdTag != null
                ? SynchronizationResult.success(createdTag, false)
                : SynchronizationResult.failure();
    }

    /** Handles synchronization errors by logging and recording the failure. */
    private SynchronizationResult handleSynchronizationError(UnexpectedHttpResponseException e,
            AviatorSSCPrepareHelper.PrepareResult result) {
        LOG.error("Error synchronizing custom tag '{}': {}", tagDef.getName(), e.getMessage());
        result.addEntry("Custom Tag", "FAILED",
                "Error synchronizing tag '" + tagDef.getName() + "': " + e.getMessage());
        return SynchronizationResult.failure();
    }

    /**
     * Returns true if this tag is an Aviator built-in tag that may be system-managed in SSC 26.2+.
     * DAST correlation tag is NOT an Aviator built-in - it's always a custom tag created by fcli.
     */
    private boolean isAviatorBuiltInTag() {
        return tagDef == AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG
                || tagDef == AviatorSSCTagDefs.AVIATOR_STATUS_TAG;
    }

    /**
     * Queries the /internalCustomTags endpoint to find system-managed Aviator tags.
     * Returns null if the endpoint doesn't exist (older SSC) or tag not found.
     */
    private JsonNode findInInternalCustomTags() {
        try {
            LOG.debug("Checking /internalCustomTags for system-managed tag '{}'", tagDef.getName());
            JsonNode response = unirest.get(SSCUrls.INTERNAL_CUSTOM_TAGS)
                    .asObject(JsonNode.class).getBody();
            JsonNode data = response.get("data");
            if (data == null || !data.isArray()) {
                LOG.debug("No data array in /internalCustomTags response");
                return null;
            }
            return JsonHelper.stream((ArrayNode) data)
                    .filter(tag -> tagDef.getGuid().equalsIgnoreCase(tag.path("guid").asText()))
                    .findFirst().orElse(null);
        } catch (Exception e) {
            // Endpoint may not exist in older SSC versions - this is expected.
            // Various exceptions can occur: HTTP 404, JSON parse errors, etc.
            LOG.debug("Could not query /internalCustomTags (may not exist in this SSC version): {}", e.getMessage());
            return null;
        }
    }

    private JsonNode verifyAndUpdateExistingTag(AviatorSSCPrepareHelper.PrepareResult result, JsonNode existingTag) {
        LOG.debug("Found existing tag '{}'. Verifying values.", tagDef.getName());
        JsonNode fullTagDetails = unirest.get(SSCUrls.CUSTOM_TAG(existingTag.get("id").asText())).asObject(JsonNode.class).getBody().get("data");

        // TEXT-type tags have no predefined valueList — nothing to verify or update
        if ("TEXT".equals(tagDef.getValueType())) {
            LOG.info("Custom tag '{}' is a TEXT type tag — no value list to verify.", tagDef.getName());
            result.addEntry("Custom Tag", "VERIFIED", "'" + tagDef.getName() + "' (TEXT type) is already present.");
            return fullTagDetails;
        }

        JsonNode valueListNode = fullTagDetails.get("valueList");
        ArrayNode valueListArray = (valueListNode != null && valueListNode.isArray())
            ? (ArrayNode) valueListNode
            : JsonHelper.getObjectMapper().createArrayNode();

        Set<String> existingValues = JsonHelper.stream(valueListArray)
            .map(v -> v.get("lookupValue").asText())
            .collect(Collectors.toSet());
        List<String> missingValues = tagDef.getValues().stream()
            .filter(v -> !existingValues.contains(v))
            .collect(Collectors.toList());

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
        tagNode.put("description", "Custom tag for Fortify Remediation Aviator.");
        tagNode.put("valueType", tagDef.getValueType());
        tagNode.put("customTagType", "CUSTOM");
        // LIST tags need a populated valueList; TEXT tags use an empty array
        ArrayNode values = tagNode.putArray("valueList");
        if (!"TEXT".equals(tagDef.getValueType())) {
            for (int i = 0; i < tagDef.getValues().size(); i++) {
                values.add(JsonHelper.getObjectMapper().createObjectNode()
                        .put("lookupValue", tagDef.getValues().get(i))
                        .put("deletable", true).put("hidden", false).put("seqNumber", i));
            }
        }
        return tagNode;
    }
}
