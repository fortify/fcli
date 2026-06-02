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
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;

import kong.unirest.UnirestInstance;

/**
 * Validates that the SSC instance has the required custom tags and tag values
 * before uploading an audited FPR. This prevents SSC from silently dropping
 * audit results and provides actionable warnings to the user.
 *
 * <p>Validates two categories of tags:
 * <ul>
 *   <li><b>Aviator custom tags</b> (Aviator prediction, Aviator status) —
 *       created by {@code aviator ssc prepare}.</li>
 *   <li><b>Analysis tag values</b> — the standard Analysis tag
 *       ({@code 87f2364f-dcd4-49e6-861d-f8d3f351686b}) must contain the values
 *       that the tag mapping config writes (e.g., "Not an Issue", "Exploitable").</li>
 * </ul>
 */
public final class AviatorSSCTagValidator {

    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCTagValidator.class);

    private AviatorSSCTagValidator() {}

    /**
     * Validates that the SSC instance has the required Aviator custom tags and
     * that the Analysis tag contains the values needed for the audit results.
     *
     * <p>Logs warnings to both the progress writer (visible on stdout) and the
     * log file. Does not throw exceptions — validation failures are advisory.
     *
     * @param unirest   active SSC session
     * @param versionId SSC application version ID to validate tags for
     * @param analysisTagId the tag ID used for writing audit results (from TagMappingConfig)
     * @param analysisTagValues the set of values the audit may write to the Analysis tag
     * @param logger    logger for progress/warnings visible to the user
     * @return list of warning messages (empty if all validations pass)
     */
    public static List<String> validatePreUpload(UnirestInstance unirest, String versionId,
            String analysisTagId, Set<String> analysisTagValues, IAviatorLogger logger) {
        LOG.info("Starting pre-upload tag validation for app version id={}. analysisTagId='{}', analysisTagValues={}",
            versionId, analysisTagId, analysisTagValues);
        logger.progress("Status: Validating SSC custom tags for app version before uploading audited FPR...");
        List<String> warnings = new ArrayList<>();
        try {
            ArrayNode versionCustomTags = fetchVersionCustomTags(unirest, versionId);
            if (versionCustomTags == null) {
                String msg = "WARN: Could not retrieve custom tags for this application version from SSC. "
                    + "Tag validation skipped — audit results may be silently dropped if 'fcli aviator ssc prepare' has not been run.";
                LOG.warn(msg);
                warnings.add(msg);
                emitWarnings(warnings, logger);
                return warnings;
            }
            LOG.info("Fetched {} custom tags for app version id={} from SSC.", versionCustomTags.size(), versionId);
            LOG.debug("Version custom tags: {}", versionCustomTags);

            validateAviatorCustomTags(versionCustomTags, warnings);
            validateAnalysisTagValues(versionCustomTags, unirest, analysisTagId, analysisTagValues, warnings);

            if (warnings.isEmpty()) {
                LOG.info("Pre-upload tag validation passed — all required tags and values are present on this app version.");
                logger.progress("Status: SSC custom tag validation passed.");
            } else {
                LOG.warn("Pre-upload tag validation found {} issue(s).", warnings.size());
                emitWarnings(warnings, logger);
            }
        } catch (Exception e) {
            String msg = "WARN: Pre-upload tag validation failed: " + e.getMessage()
                + ". Proceeding with upload — audit results may be silently dropped by SSC.";
            LOG.warn(msg, e);
            warnings.add(msg);
            emitWarnings(warnings, logger);
        }
        return warnings;
    }

    /**
     * Emits each warning via the progress writer so they are visible on stdout.
     * Each warning is emitted as a separate progress message.
     */
    private static void emitWarnings(List<String> warnings, IAviatorLogger logger) {
        for (String warning : warnings) {
            logger.warn(warning);
            //logger.progress(warning);
        }
    }

    private static ArrayNode fetchVersionCustomTags(UnirestInstance unirest, String versionId) {
        String url = SSCUrls.PROJECT_VERSION_CUSTOM_TAGS(versionId);
        LOG.debug("Fetching custom tags for app version from SSC via {}", url);
        JsonNode body = unirest.get(url)
            .queryString("limit", "-1")
            .asObject(JsonNode.class).getBody();
        LOG.debug("SSC version custom tags response body: {}", body);
        JsonNode data = body.get("data");
        if (data == null || !data.isArray()) {
            LOG.warn("SSC version custom tags response has no 'data' array. body={}", body);
            return null;
        }
        return (ArrayNode) data;
    }

    /**
     * Checks that both Aviator custom tags (Aviator prediction, Aviator status)
     * exist on the SSC instance. These are created by {@code aviator ssc prepare}.
     */
    private static void validateAviatorCustomTags(ArrayNode allCustomTags, List<String> warnings) {
        validateTagExists(allCustomTags, AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG, warnings);
        validateTagExists(allCustomTags, AviatorSSCTagDefs.AVIATOR_STATUS_TAG, warnings);
    }

    private static void validateTagExists(ArrayNode versionCustomTags, AviatorSSCTagDefs.TagDefinition tagDef,
            List<String> warnings) {
        LOG.debug("Looking for custom tag '{}' with GUID '{}' among {} app version tags",
            tagDef.getName(), tagDef.getGuid(), versionCustomTags.size());

        boolean found = JsonHelper.stream(versionCustomTags)
            .anyMatch(tag -> {
                String tagGuid = tag.path("guid").asText();
                LOG.trace("Comparing version tag guid='{}' with expected guid='{}'", tagGuid, tagDef.getGuid());
                return tagDef.getGuid().equals(tagGuid);
            });

        if (found) {
            LOG.info("Custom tag '{}' (GUID: {}) is associated with this app version — OK.", tagDef.getName(), tagDef.getGuid());
        } else {
            String msg = String.format(
                "WARN: Custom tag '%s' (GUID: %s) is not associated with this application version. "
                    + "Audit results for this tag will not be visible in SSC. "
                    + "Run 'fcli aviator ssc prepare' to resolve this.",
                tagDef.getName(), tagDef.getGuid());
            LOG.warn(msg);
            warnings.add(msg);
        }
    }

    /**
     * Checks that the Analysis tag (or custom tag used for audit results) exists
     * on the SSC application version and contains all required values. If the
     * Analysis tag is a LIST type, missing values mean SSC will silently drop
     * those audit decisions.
     */
    private static void validateAnalysisTagValues(ArrayNode versionCustomTags, UnirestInstance unirest,
            String analysisTagId, Set<String> requiredValues, List<String> warnings) {
        LOG.debug("Validating Analysis tag values. analysisTagId='{}', requiredValues={}", analysisTagId, requiredValues);

        if (analysisTagId == null || analysisTagId.isBlank() || requiredValues == null || requiredValues.isEmpty()) {
            LOG.debug("Skipping Analysis tag validation: analysisTagId={}, requiredValues={}", analysisTagId, requiredValues);
            return;
        }

        // Find the Analysis tag by GUID in the version's custom tags
        JsonNode analysisTag = JsonHelper.stream(versionCustomTags)
            .filter(tag -> analysisTagId.equalsIgnoreCase(tag.path("guid").asText()))
            .findFirst().orElse(null);

        if (analysisTag == null) {
            LOG.debug("Analysis tag with GUID '{}' not found in app version custom tags. "
                + "Available GUIDs: {}", analysisTagId,
                JsonHelper.stream(versionCustomTags).map(t -> t.path("guid").asText()).toList());
            String msg = String.format(
                "WARN: Analysis tag (GUID: %s) is not associated with this application version. "
                    + "Audit results written to this tag will not be visible in SSC.",
                analysisTagId);
            LOG.warn(msg);
            warnings.add(msg);
            return;
        }

        LOG.debug("Found Analysis tag: id={}, name='{}', valueType='{}'",
            analysisTag.path("id").asText(), analysisTag.path("name").asText(),
            analysisTag.path("valueType").asText());

        // Only validate values for LIST type tags
        String valueType = analysisTag.path("valueType").asText("");
        if (!"LIST".equalsIgnoreCase(valueType)) {
            LOG.debug("Analysis tag valueType='{}' is not LIST — skipping value validation.", valueType);
            return;
        }

        // Fetch full tag details to get valueList
        String tagId = analysisTag.path("id").asText();
        LOG.debug("Fetching full tag details for Analysis tag id={}", tagId);
        JsonNode fullTagDetails = unirest.get(SSCUrls.CUSTOM_TAG(tagId))
            .asObject(JsonNode.class).getBody().path("data");
        LOG.debug("Full Analysis tag details: {}", fullTagDetails);

        JsonNode valueListNode = fullTagDetails.get("valueList");
        if (valueListNode == null || !valueListNode.isArray()) {
            String msg = String.format(
                "WARN: Analysis tag '%s' has no value list configured. "
                    + "Audit results written to this tag may be silently dropped by SSC.",
                fullTagDetails.path("name").asText("Analysis"));
            LOG.warn(msg);
            warnings.add(msg);
            return;
        }

        Set<String> existingValues = JsonHelper.stream((ArrayNode) valueListNode)
            .map(v -> v.path("lookupValue").asText())
            .collect(Collectors.toSet());
        LOG.debug("Analysis tag existing values: {}", existingValues);
        LOG.debug("Required values: {}", requiredValues);

        List<String> missingValues = requiredValues.stream()
            .filter(v -> v != null && !v.isBlank())
            .filter(v -> !existingValues.contains(v))
            .toList();

        if (!missingValues.isEmpty()) {
            String tagName = fullTagDetails.path("name").asText("Analysis");
            String msg = String.format(
                "WARN: Analysis tag '%s' (GUID: %s) is missing the following values: %s. "
                    + "These audit results will not be reflected in SSC. "
                    + "Verify the tag configuration or use --tag-mapping to customize value mapping.",
                tagName, analysisTagId, missingValues);
            LOG.warn(msg);
            warnings.add(msg);
        } else {
            LOG.info("Analysis tag '{}' has all required values — OK.", fullTagDetails.path("name").asText("Analysis"));
        }
    }
}
