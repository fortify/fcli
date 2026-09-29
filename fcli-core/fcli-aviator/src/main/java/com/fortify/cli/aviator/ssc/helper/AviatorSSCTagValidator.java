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
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCTagDefs.TagDefinition;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

/**
 * Checks SSC custom tags before an audited FPR is uploaded.
 *
 * <p>Aviator prediction and Aviator status are accepted when the application version
 * has them assigned, or when SSC returns them as built-in Aviator tags. The Analysis
 * tag, when the audit writes to it, must be assigned and must already contain the
 * mapped values. Each problem is reported as a warning. Validation does not stop the upload.
 */
public final class AviatorSSCTagValidator {

    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCTagValidator.class);
    private static final String AVIATOR_TAG_TYPE = "AVIATOR";
    private static final List<TagDefinition> AVIATOR_TAGS = List.of(
        AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG,
        AviatorSSCTagDefs.AVIATOR_STATUS_TAG);
    private static final String ENABLE_AVIATOR =
        "Enable Aviator in SSC under Administration -> Configuration -> AI Assistant -> Aviator.";
    private static final String RUN_PREPARE = "Run 'fcli aviator ssc prepare' to resolve this.";

    private AviatorSSCTagValidator() {}

    /**
     * Checks Aviator tags and Analysis values for one application version.
     *
     * <p>Warnings are written to {@code logger} and returned. An empty list means every
     * required tag and value is present. A failure to read tags from SSC is reported as
     * a warning and is not thrown.
     *
     * @param unirest active SSC session
     * @param versionId application version whose tags are checked
     * @param analysisTagId GUID of the tag that receives audit results, or blank when that check is not required
     * @param analysisTagValues values the audit may write to {@code analysisTagId}
     * @param logger receives progress messages and warnings shown to the user
     * @return warnings, empty when validation passes
     */
    public static List<String> validatePreUpload(UnirestInstance unirest, String versionId,
            String analysisTagId, Set<String> analysisTagValues, IAviatorLogger logger) {
        LOG.info("Starting pre-upload tag validation for app version id={}. analysisTagId='{}', analysisTagValues={}",
            versionId, analysisTagId, analysisTagValues);
        logger.progress("Status: Validating SSC custom tags for app version before uploading audited FPR...");
        List<String> warnings = new ArrayList<>();
        try {
            ArrayNode assignedTags = fetchCustomTags(unirest, versionId, false);
            if (assignedTags == null) {
                warnings.add("WARN: Could not retrieve custom tags for this application version from SSC. "
                    + "Tag validation skipped — audit results may be silently dropped if 'fcli aviator ssc prepare' has not been run.");
                emitWarnings(warnings, logger);
                return warnings;
            }
            LOG.info("Fetched {} custom tags for app version id={} from SSC.", assignedTags.size(), versionId);
            LOG.debug("Version custom tags: {}", assignedTags);

            validateAviatorTags(assignedTags, unirest, versionId, warnings);
            validateAnalysisTag(assignedTags, unirest, analysisTagId, analysisTagValues, warnings);

            if (warnings.isEmpty()) {
                LOG.info("Pre-upload tag validation passed — all required tags and values are present on this app version.");
                logger.progress("Status: SSC custom tag validation passed.");
            } else {
                LOG.debug("Pre-upload tag validation found {} issue(s).", warnings.size());
                emitWarnings(warnings, logger);
            }
        } catch (Exception e) {
            String msg = "WARN: Pre-upload tag validation failed: " + e.getMessage()
                + ". Proceeding with upload — audit results may be silently dropped by SSC.";
            LOG.debug(msg, e);
            warnings.add(msg);
            emitWarnings(warnings, logger);
        }
        return warnings;
    }

    /**
     * Writes each collected warning once.
     */
    private static void emitWarnings(List<String> warnings, IAviatorLogger logger) {
        for (String warning : warnings) {
            logger.warn(warning);
        }
    }

    /**
     * Returns custom tags for the application version.
     * Uses {@link SSCAppVersionHelper#getCustomTagsRequest}. {@code includeAll} also
     * returns built-in tags that are not assigned to the version.
     *
     * @return the tag array, or {@code null} when the response has no data array
     */
    private static ArrayNode fetchCustomTags(UnirestInstance unirest, String versionId, boolean includeAll) {
        LOG.debug("Fetching custom tags for app version id={} includeall={}", versionId, includeAll);
        GetRequest request = SSCAppVersionHelper.getCustomTagsRequest(unirest, versionId);
        if (includeAll) {
            request = request.queryString("includeall", "true");
        }
        JsonNode body = request.asObject(JsonNode.class).getBody();
        LOG.debug("SSC version custom tags response body: {}", body);
        ArrayNode tags = dataArray(body);
        if (tags == null) {
            LOG.debug("SSC version custom tags response has no data array. includeall={}", includeAll);
        }
        return tags;
    }

    /**
     * Adds a warning for each Aviator tag that is neither assigned nor built-in.
     * The include-all list is read only after the assigned list is missing a tag.
     * A failed include-all request leaves those tags unresolved. The assigned-tag
     * request is not optional: its failure stops validation.
     * A tag that is still missing tells the user to enable Aviator when SSC already
     * publishes it as built-in, and to run prepare otherwise.
     */
    private static void validateAviatorTags(ArrayNode assignedTags, UnirestInstance unirest,
            String versionId, List<String> warnings) {
        List<TagDefinition> missing = new ArrayList<>();
        for (TagDefinition tag : AVIATOR_TAGS) {
            if (containsGuid(assignedTags, tag.getGuid())) {
                LOG.info("Custom tag '{}' (GUID: {}) is associated with this app version — OK.",
                    tag.getName(), tag.getGuid());
            } else {
                missing.add(tag);
            }
        }
        if (missing.isEmpty()) {
            return;
        }

        ArrayNode includeAllTags;
        try {
            includeAllTags = fetchCustomTags(unirest, versionId, true);
        } catch (Exception e) {
            LOG.debug("Could not retrieve custom tags with includeall=true for app version id={}: {}",
                versionId, e.getMessage());
            includeAllTags = null;
        }
        List<TagDefinition> unresolved = new ArrayList<>();
        for (TagDefinition tag : missing) {
            if (containsBuiltInTag(includeAllTags, tag)) {
                LOG.info("Custom tag '{}' (GUID: {}) is a built-in Aviator tag (customTagType AVIATOR) — OK.",
                    tag.getName(), tag.getGuid());
            } else {
                unresolved.add(tag);
            }
        }
        if (unresolved.isEmpty()) {
            return;
        }

        String resolution = publishesBuiltInAviatorTags(unirest) ? ENABLE_AVIATOR : RUN_PREPARE;
        for (TagDefinition tag : unresolved) {
            warnings.add(missingTagWarning(tag, resolution));
        }
    }

    /**
     * Returns whether SSC publishes Aviator prediction or Aviator status as a built-in tag.
     * Older SSC exposes the same catalog without those tags, so the catalog alone is not enough.
     * Returns {@code false} when the request fails, which keeps the prepare guidance.
     */
    private static boolean publishesBuiltInAviatorTags(UnirestInstance unirest) {
        try {
            LOG.debug("Checking /internalCustomTags for built-in Aviator tags");
            JsonNode body = unirest.get(SSCUrls.INTERNAL_CUSTOM_TAGS).asObject(JsonNode.class).getBody();
            ArrayNode tags = dataArray(body);
            boolean present = tags != null && JsonHelper.stream(tags).anyMatch(AviatorSSCTagValidator::isBuiltInAviatorTag);
            LOG.debug("SSC /internalCustomTags has built-in Aviator tags={}", present);
            return present;
        } catch (Exception e) {
            LOG.debug("Could not query /internalCustomTags: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns whether {@code tags} contains {@code tagDef} with custom tag type {@code AVIATOR}.
     */
    private static boolean containsBuiltInTag(ArrayNode tags, TagDefinition tagDef) {
        return tags != null && JsonHelper.stream(tags).anyMatch(tag -> isBuiltInTag(tag, tagDef));
    }

    /**
     * Returns whether {@code tag} is Aviator prediction or Aviator status with custom tag type {@code AVIATOR}.
     */
    private static boolean isBuiltInAviatorTag(JsonNode tag) {
        return AVIATOR_TAGS.stream().anyMatch(tagDef -> isBuiltInTag(tag, tagDef));
    }

    /**
     * Returns whether {@code tag} is {@code tagDef} and its custom tag type is {@code AVIATOR}.
     */
    private static boolean isBuiltInTag(JsonNode tag, TagDefinition tagDef) {
        return tagDef.getGuid().equalsIgnoreCase(tag.path("guid").asText())
            && AVIATOR_TAG_TYPE.equalsIgnoreCase(tag.path("customTagType").asText());
    }

    /**
     * Returns the warning for an Aviator tag that is not available on the application version.
     *
     * @param resolution guidance that tells the user how to make the tag available
     */
    private static String missingTagWarning(TagDefinition tagDef, String resolution) {
        return String.format(
            "WARN: Custom tag '%s' (GUID: %s) is not associated with this application version. "
                + "Audit results for this tag will not be visible in SSC. %s",
            tagDef.getName(), tagDef.getGuid(), resolution);
    }

    /**
     * Adds a warning when the Analysis tag is missing, or when a list-valued Analysis tag
     * does not contain the values the audit will write. Does nothing when no Analysis tag
     * or values were requested. Only tags assigned to the version are considered.
     */
    private static void validateAnalysisTag(ArrayNode assignedTags, UnirestInstance unirest,
            String analysisTagId, Set<String> requiredValues, List<String> warnings) {
        LOG.debug("Validating Analysis tag values. analysisTagId='{}', requiredValues={}", analysisTagId, requiredValues);
        if (analysisTagId == null || analysisTagId.isBlank() || requiredValues == null || requiredValues.isEmpty()) {
            LOG.debug("Skipping Analysis tag validation: analysisTagId={}, requiredValues={}", analysisTagId, requiredValues);
            return;
        }

        JsonNode analysisTag = findByGuid(assignedTags, analysisTagId);
        if (analysisTag == null) {
            LOG.debug("Analysis tag with GUID '{}' not found in app version custom tags. Available GUIDs: {}",
                analysisTagId, JsonHelper.stream(assignedTags).map(tag -> tag.path("guid").asText()).toList());
            warnings.add(String.format(
                "WARN: Analysis tag (GUID: %s) is not associated with this application version. "
                    + "Audit results written to this tag will not be visible in SSC.",
                analysisTagId));
            return;
        }

        String valueType = analysisTag.path("valueType").asText("");
        LOG.debug("Found Analysis tag: id={}, name='{}', valueType='{}'",
            analysisTag.path("id").asText(), analysisTag.path("name").asText(), valueType);
        if (!"LIST".equalsIgnoreCase(valueType)) {
            LOG.debug("Analysis tag valueType='{}' is not LIST — skipping value validation.", valueType);
            return;
        }
        warnForMissingAnalysisValues(unirest, analysisTag, analysisTagId, requiredValues, warnings);
    }

    /**
     * Adds a warning when the Analysis value list is missing or does not contain {@code requiredValues}.
     */
    private static void warnForMissingAnalysisValues(UnirestInstance unirest, JsonNode analysisTag,
            String analysisTagId, Set<String> requiredValues, List<String> warnings) {
        String tagId = analysisTag.path("id").asText();
        LOG.debug("Fetching full tag details for Analysis tag id={}", tagId);
        JsonNode tagDetails = unirest.get(SSCUrls.CUSTOM_TAG(tagId)).asObject(JsonNode.class).getBody().path("data");
        LOG.debug("Full Analysis tag details: {}", tagDetails);

        JsonNode valueList = tagDetails.get("valueList");
        if (valueList == null || !valueList.isArray()) {
            warnings.add(String.format(
                "WARN: Analysis tag '%s' has no value list configured. "
                    + "Audit results written to this tag may be silently dropped by SSC.",
                tagDetails.path("name").asText("Analysis")));
            return;
        }

        Set<String> existingValues = JsonHelper.stream((ArrayNode) valueList)
            .map(value -> value.path("lookupValue").asText())
            .collect(Collectors.toSet());
        LOG.debug("Analysis tag existing values: {}", existingValues);
        LOG.debug("Required values: {}", requiredValues);

        List<String> missingValues = requiredValues.stream()
            .filter(value -> value != null && !value.isBlank())
            .filter(value -> !existingValues.contains(value))
            .toList();
        if (missingValues.isEmpty()) {
            LOG.info("Analysis tag '{}' has all required values — OK.", tagDetails.path("name").asText("Analysis"));
            return;
        }
        warnings.add(String.format(
            "WARN: Analysis tag '%s' (GUID: %s) is missing the following values: %s. "
                + "These audit results will not be reflected in SSC. "
                + "Verify the tag configuration or use --tag-mapping to customize value mapping.",
            tagDetails.path("name").asText("Analysis"), analysisTagId, missingValues));
    }

    /**
     * Returns whether {@code tags} contains {@code guid}.
     */
    private static boolean containsGuid(ArrayNode tags, String guid) {
        return findByGuid(tags, guid) != null;
    }

    /**
     * Returns the first tag whose GUID matches {@code guid}, or {@code null} when none does.
     */
    private static JsonNode findByGuid(ArrayNode tags, String guid) {
        if (tags == null || guid == null) {
            return null;
        }
        return JsonHelper.stream(tags)
            .filter(tag -> guid.equalsIgnoreCase(tag.path("guid").asText()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Returns the {@code data} array from an SSC response, or {@code null} when that array is absent.
     */
    private static ArrayNode dataArray(JsonNode body) {
        JsonNode data = body == null ? null : body.get("data");
        return data != null && data.isArray() ? (ArrayNode) data : null;
    }
}
