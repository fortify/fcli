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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCTagDefs.TagDefinition;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

/**
 * Checks SSC custom tags before an audited FPR is uploaded, and owns the decision whether a
 * SAST audit must use the legacy Aviator status for issues with remediation.
 *
 * <p>One {@code includeall=true} response supplies the tags. An assigned tag has type
 * {@code CUSTOM}. A built-in Aviator tag has type {@code AVIATOR}. Either one makes
 * Aviator prediction and Aviator status available. The Analysis tag, when the audit
 * writes to it, must be in that response and must already contain the mapped values.
 * Each problem is reported as a warning. Validation does not stop the upload.
 */
public final class AviatorSSCTagValidator {

    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCTagValidator.class);
    private static final List<TagDefinition> AVIATOR_TAGS = List.of(
        AviatorSSCTagDefs.AVIATOR_PREDICTION_TAG,
        AviatorSSCTagDefs.AVIATOR_STATUS_TAG);
    private static final String ENABLE_AVIATOR =
        "Enable Aviator in SSC under Administration -> Configuration -> AI Assistant -> Aviator.";
    private static final String RUN_PREPARE = "Run 'fcli aviator ssc prepare' to resolve this.";
    private static final String STATUS_SUPPORT_UNKNOWN = " Issues with remediation will be marked '"
        + Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION + "', which may not be visible in SSC.";

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
     * @param statusValues Aviator status values the audit may write that must be checked;
     *        empty to skip the value check (see {@link #sastStatusValues(boolean)})
     * @param logger receives progress messages and warnings shown to the user
     * @return warnings, empty when validation passes
     */
    public static List<String> validatePreUpload(UnirestInstance unirest, String versionId,
            String analysisTagId, Set<String> analysisTagValues, List<String> statusValues,
            IAviatorLogger logger) {
        LOG.info("Starting pre-upload tag validation for app version id={}. analysisTagId='{}', analysisTagValues={}",
            versionId, analysisTagId, analysisTagValues);
        logger.progress("Status: Validating SSC custom tags for app version before uploading audited FPR...");
        List<String> warnings = new ArrayList<>();
        try {
            ArrayNode tags = fetchCustomTags(unirest, versionId);
            if (tags == null) {
                warnings.add("WARN: Could not retrieve custom tags for this application version from SSC. "
                    + "Tag validation skipped — audit results may be silently dropped if 'fcli aviator ssc prepare' has not been run.");
                emitWarnings(warnings, logger);
                return warnings;
            }
            LOG.info("Fetched {} custom tags for app version id={} from SSC.", tags.size(), versionId);
            LOG.debug("Version custom tags: {}", tags);

            validateAviatorTags(tags, unirest, warnings, statusValues);
            validateAnalysisTag(tags, unirest, analysisTagId, analysisTagValues, warnings);

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
     * Returns the Aviator status values a SAST audit may write, given the result of
     * {@link #shouldUseLegacySastStatus}.
     */
    public static List<String> sastStatusValues(boolean useLegacyStatus) {
        return useLegacyStatus ? List.of(Constants.PROCESSED_BY_AVIATOR) : Constants.AVIATOR_STATUS_VALUES;
    }

    /**
     * Decides whether a SAST audit must mark issues with remediation as {@link Constants#PROCESSED_BY_AVIATOR}.
     *
     * <p>Returns {@code true} only when the Aviator status tag is system-managed, its value list is
     * known, it contains {@link Constants#PROCESSED_BY_AVIATOR}, and it lacks
     * {@link Constants#PROCESSED_BY_AVIATOR_WITH_REMEDIATION}. A custom status tag returns {@code false};
     * prepare can add the missing value, and pre-upload validation reports it. When the status
     * support cannot be determined, a warning is written and {@code false} is returned so the audit continues.
     */
    public static boolean shouldUseLegacySastStatus(UnirestInstance unirest, String versionId, IAviatorLogger logger) {
        try {
            ArrayNode tags = fetchCustomTags(unirest, versionId);
            if (tags == null) {
                logger.warn("WARN: Could not check SSC 'Aviator status' support before the audit: "
                    + "SSC returned no custom tags." + STATUS_SUPPORT_UNKNOWN);
                return false;
            }
            JsonNode statusTag = AviatorSSCTagJsonHelper.findByGuid(tags, AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid());
            if (!AviatorSSCTagJsonHelper.isSystemManaged(statusTag, AviatorSSCTagDefs.AVIATOR_STATUS_TAG)) {
                return false;
            }
            JsonNode details = getStatusTagDetails(unirest, statusTag);
            Set<String> values = AviatorSSCTagJsonHelper.lookupValues(details);
            if (values == null || !AviatorSSCTagJsonHelper.isSystemManaged(details, AviatorSSCTagDefs.AVIATOR_STATUS_TAG)) {
                logger.warn("WARN: Could not check the values of the system-managed 'Aviator status' tag before the audit."
                    + STATUS_SUPPORT_UNKNOWN);
                return false;
            }
            boolean useLegacyStatus = values.contains(Constants.PROCESSED_BY_AVIATOR)
                && !values.contains(Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION);
            LOG.info("System-managed 'Aviator status' values={}, legacy SAST status fallback={}", values, useLegacyStatus);
            return useLegacyStatus;
        } catch (Exception e) {
            LOG.debug("Could not check SSC 'Aviator status' support", e);
            logger.warn("%s", "WARN: Could not check SSC 'Aviator status' support before the audit: " + e.getMessage()
                + "." + STATUS_SUPPORT_UNKNOWN);
            return false;
        }
    }

    /**
     * Returns the warning for issues that were marked with the legacy status because of
     * {@link #shouldUseLegacySastStatus}.
     */
    public static String legacyStatusFallbackWarning(int fallbackCount) {
        return String.format("WARN: %d issue(s) with remediation were marked '%s' instead of '%s', because the "
            + "system-managed 'Aviator status' tag in SSC did not support '%s' when the audit started. "
            + "The fallback does not change remediation generation, but SSC will not distinguish these issues "
            + "from audits without remediation. "
            + "Configure the system-managed tag through SSC to distinguish them in future audits.",
            fallbackCount, Constants.PROCESSED_BY_AVIATOR, Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION,
            Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION);
    }

    /**
     * Writes each collected warning once.
     */
    private static void emitWarnings(List<String> warnings, IAviatorLogger logger) {
        for (String warning : warnings) {
            logger.warn("%s", warning);
        }
    }

    /**
     * Returns the application version's custom tags, including built-in tags.
     * Uses {@link SSCAppVersionHelper#getCustomTagsRequest} with {@code includeall=true}.
     * That response contains assigned {@code CUSTOM} tags and internal tags such as Aviator.
     *
     * @return the tag array, or {@code null} when the response has no data array
     */
    private static ArrayNode fetchCustomTags(UnirestInstance unirest, String versionId) {
        LOG.debug("Fetching custom tags for app version id={} includeall=true", versionId);
        GetRequest request = SSCAppVersionHelper.getCustomTagsRequest(unirest, versionId)
            .queryString("includeall", "true");
        JsonNode body = request.asObject(JsonNode.class).getBody();
        LOG.debug("SSC version custom tags response body: {}", body);
        ArrayNode tags = AviatorSSCTagJsonHelper.dataArray(body);
        if (tags == null) {
            LOG.debug("SSC version custom tags response has no data array.");
        }
        return tags;
    }

    /**
     * Adds a warning for each Aviator tag missing from {@code tags}.
     * A {@code CUSTOM} entry is assigned to the version. An {@code AVIATOR} entry is built-in.
     * Either one is enough. A missing tag tells the user to enable Aviator when SSC publishes
     * that tag as built-in, and to run prepare otherwise.
     */
    private static void validateAviatorTags(ArrayNode tags, UnirestInstance unirest, List<String> warnings,
            List<String> statusValues) {
        List<TagDefinition> missing = new ArrayList<>();
        for (TagDefinition tag : AVIATOR_TAGS) {
            if (AviatorSSCTagJsonHelper.findByGuid(tags, tag.getGuid()) != null) {
                LOG.info("Custom tag '{}' (GUID: {}) is available on this app version — OK.",
                    tag.getName(), tag.getGuid());
            } else {
                missing.add(tag);
            }
        }
        if (!missing.isEmpty()) {
            String resolution = publishesBuiltInAviatorTags(unirest) ? ENABLE_AVIATOR : RUN_PREPARE;
            for (TagDefinition tag : missing) {
                warnings.add(missingTagWarning(tag, resolution));
            }
        }
        JsonNode statusTag = AviatorSSCTagJsonHelper.findByGuid(tags, AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid());
        if (statusValues != null && !statusValues.isEmpty() && statusTag != null) {
            validateAviatorStatusValues(statusTag, unirest, warnings, statusValues);
        }
    }

    /**
     * Warns when the Aviator status tag cannot accept {@code statusValues}.
     */
    private static void validateAviatorStatusValues(JsonNode statusTag, UnirestInstance unirest,
            List<String> warnings, List<String> statusValues) {
        Set<String> existingValues = AviatorSSCTagJsonHelper.lookupValues(getStatusTagDetails(unirest, statusTag));
        if (existingValues == null) {
            warnings.add("WARN: Could not verify the value list for 'Aviator status'. "
                + "Audit results may not display correctly in SSC.");
            return;
        }
        List<String> missingValues = statusValues.stream()
            .filter(value -> !existingValues.contains(value))
            .toList();
        if (missingValues.isEmpty()) {
            return;
        }

        String resolution = AviatorSSCTagJsonHelper.isSystemManaged(statusTag, AviatorSSCTagDefs.AVIATOR_STATUS_TAG)
            ? "Configure the system-managed tag through SSC."
            : RUN_PREPARE;
        warnings.add(String.format(
            "WARN: 'Aviator status' is missing values %s. %s Audit results for these values may not be visible in SSC.",
            missingValues, resolution));
    }

    /**
     * Returns the Aviator status tag with its value list: the version entry when it already has one,
     * the {@code /internalCustomTags} entry for a system-managed tag, or the custom tag details otherwise.
     * Returns {@code null} when the details cannot be located. HTTP failures propagate.
     */
    private static JsonNode getStatusTagDetails(UnirestInstance unirest, JsonNode statusTag) {
        if (AviatorSSCTagJsonHelper.lookupValues(statusTag) != null) {
            return statusTag;
        }
        if (AviatorSSCTagJsonHelper.isSystemManaged(statusTag, AviatorSSCTagDefs.AVIATOR_STATUS_TAG)) {
            return AviatorSSCTagJsonHelper.findByGuid(
                AviatorSSCTagJsonHelper.fetchInternalCustomTags(unirest), AviatorSSCTagDefs.AVIATOR_STATUS_TAG.getGuid());
        }
        String tagId = statusTag.path("id").asText();
        if (tagId.isBlank()) {
            return null;
        }
        JsonNode response = unirest.get(SSCUrls.CUSTOM_TAG(tagId)).asObject(JsonNode.class).getBody();
        return response == null ? null : response.get("data");
    }

    /**
     * Returns whether SSC publishes Aviator prediction or Aviator status as a built-in tag.
     * Older SSC exposes the same catalog without those tags, so the catalog alone is not enough.
     * Returns {@code false} when the request fails for any reason, which keeps the prepare guidance.
     */
    private static boolean publishesBuiltInAviatorTags(UnirestInstance unirest) {
        try {
            LOG.debug("Checking /internalCustomTags for built-in Aviator tags");
            ArrayNode tags = AviatorSSCTagJsonHelper.fetchInternalCustomTags(unirest);
            boolean present = tags != null && JsonHelper.stream(tags).anyMatch(tag -> AVIATOR_TAGS.stream()
                .anyMatch(tagDef -> AviatorSSCTagJsonHelper.isSystemManaged(tag, tagDef)));
            LOG.debug("SSC /internalCustomTags has built-in Aviator tags={}", present);
            return present;
        } catch (Exception e) {
            LOG.debug("Could not query /internalCustomTags: {}", e.getMessage());
            return false;
        }
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
     * or values were requested. An Analysis tag in {@code tags} is assigned to the version.
     */
    private static void validateAnalysisTag(ArrayNode tags, UnirestInstance unirest,
            String analysisTagId, Set<String> requiredValues, List<String> warnings) {
        LOG.debug("Validating Analysis tag values. analysisTagId='{}', requiredValues={}", analysisTagId, requiredValues);
        if (analysisTagId == null || analysisTagId.isBlank() || requiredValues == null || requiredValues.isEmpty()) {
            LOG.debug("Skipping Analysis tag validation: analysisTagId={}, requiredValues={}", analysisTagId, requiredValues);
            return;
        }

        JsonNode analysisTag = AviatorSSCTagJsonHelper.findByGuid(tags, analysisTagId);
        if (analysisTag == null) {
            LOG.debug("Analysis tag with GUID '{}' not found in app version custom tags. Available GUIDs: {}",
                analysisTagId, JsonHelper.stream(tags).map(tag -> tag.path("guid").asText()).toList());
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

        Set<String> existingValues = AviatorSSCTagJsonHelper.lookupValues(tagDetails);
        if (existingValues == null) {
            warnings.add(String.format(
                "WARN: Analysis tag '%s' has no value list configured. "
                    + "Audit results written to this tag may be silently dropped by SSC.",
                tagDetails.path("name").asText("Analysis")));
            return;
        }

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
}
