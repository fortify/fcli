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
package com.fortify.cli.ssc.artifact.helper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class SSCArtifactHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SSCArtifactHelper.class);
    public static final int DEFAULT_POLL_INTERVAL_SECONDS = 1;

    private SSCArtifactHelper() {}

    public static final SSCArtifactDescriptor getArtifactDescriptor(UnirestInstance unirest, String artifactId) {
        return getDescriptor(getArtifactJsonNode(unirest, artifactId));
    }

    /**
     * Get the latest Aviator-processed artifact for an application version,
     * optionally filtered to only consider artifacts uploaded on or after sinceDate.
     *
     * @param unirest      UnirestInstance
     * @param appVersionId Application version ID
     * @param sinceDate    Optional cutoff; only artifacts with uploadDate >= sinceDate are considered. May be null.
     * @return SSCArtifactDescriptor of the most recent qualifying Aviator artifact
     * @throws FcliSimpleException if no matching Aviator artifacts found
     */
    public static final SSCArtifactDescriptor getLatestAviatorArtifact(UnirestInstance unirest, String appVersionId, OffsetDateTime sinceDate) {
        List<SSCArtifactDescriptor> artifacts = fetchAviatorArtifacts(unirest, appVersionId, sinceDate, 1);
        if (artifacts.isEmpty()) {
            throw new FcliSimpleException(buildNoArtifactsMessage(appVersionId, sinceDate));
        }
        return artifacts.get(0);
    }

    /**
     * Get all Aviator-processed artifacts for an application version, in chronological order (oldest first),
     * optionally filtered to only include artifacts uploaded on or after sinceDate.
     * Uses client-side filtering since SSC does not support server-side filtering by originalFileName.
     * Supports paging to handle application versions with many artifacts.
     *
     * Performance optimization: Uses DESC ordering to enable early termination when sinceDate is provided,
     * then reverses the result to maintain ascending order contract.
     *
     * @param urirest      UnirestInstance
     * @param appVersionId Application version ID
     * @param sinceDate    Optional cutoff; only artifacts with uploadDate >= sinceDate are included. May be null.
     * @return List of SSCArtifactDescriptor ordered by uploadDate ascending
     * @throws FcliSimpleException if no matching Aviator artifacts found
     */
    public static final List<SSCArtifactDescriptor> getAllAviatorArtifacts(UnirestInstance unirest, String appVersionId, OffsetDateTime sinceDate) {
        List<SSCArtifactDescriptor> artifacts = fetchAviatorArtifacts(unirest, appVersionId, sinceDate, Integer.MAX_VALUE);
        if (artifacts.isEmpty()) {
            throw new FcliSimpleException(buildNoArtifactsMessage(appVersionId, sinceDate));
        }
        // Reverse to maintain ascending order contract (DESC fetch → ASC result)
        java.util.Collections.reverse(artifacts);
        return artifacts;
    }

    /**
     * Fetch Aviator-processed artifacts with optimized DESC ordering for early termination.
     *
     * @param unirest      UnirestInstance
     * @param appVersionId Application version ID
     * @param sinceDate    Optional cutoff; only artifacts with uploadDate >= sinceDate. May be null.
     * @param maxResults   Maximum number of results to return (use Integer.MAX_VALUE for all)
     * @return List of SSCArtifactDescriptor in DESC order (newest first)
     */
    private static List<SSCArtifactDescriptor> fetchAviatorArtifacts(UnirestInstance unirest, String appVersionId,
                                                                      OffsetDateTime sinceDate, int maxResults) {
        List<SSCArtifactDescriptor> result = new ArrayList<>();
        int start = 0;
        int pageSize = 50;
        boolean shouldStop = false;

        while (!shouldStop && result.size() < maxResults) {
            JsonNode response = unirest.get(SSCUrls.PROJECT_VERSION_ARTIFACTS(appVersionId))
                    .queryString("orderby", "uploadDate DESC")
                    .queryString("start", start)
                    .queryString("limit", pageSize)
                    .queryString("embed", "scans")
                    .asObject(JsonNode.class)
                    .getBody();

            JsonNode data = response.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) { break; }

            // Throw exception if first page is empty (no artifacts at all)
            if (start == 0 && data.size() == 0) {
                throw new FcliSimpleException("No artifacts found for application version ID: " + appVersionId);
            }

            for (JsonNode artifact : data) {
                if (shouldStopProcessing(artifact, sinceDate)) {
                    shouldStop = true;
                    break;
                }

                if (isAviatorArtifact(artifact)) {
                    result.add(getDescriptor(artifact));
                    if (result.size() >= maxResults) {
                        shouldStop = true;
                        break;
                    }
                }
            }

            // Check if we've processed all available artifacts
            int totalCount = response.path("count").asInt(0);
            start += pageSize;
            if (start >= totalCount) { break; }
        }

        return result;
    }

    /**
     * Check if we should stop processing artifacts based on date cutoff.
     * Since artifacts are in DESC order, if current artifact is too old, all subsequent ones will be too.
     */
    private static boolean shouldStopProcessing(JsonNode artifact, OffsetDateTime sinceDate) {
        return sinceDate != null && !isUploadDateOnOrAfter(artifact, sinceDate);
    }

    /**
     * Check if artifact is Aviator-processed based on filename prefix.
     */
    private static boolean isAviatorArtifact(JsonNode artifact) {
        String originalFileName = artifact.path("originalFileName").asText("");
        return originalFileName.startsWith("aviator_");
    }

    /**
     * Check if an artifact's uploadDate is on or after the specified cutoff date.
     * Returns false if the uploadDate is missing, blank, or cannot be parsed.
     * Logs a warning for unparseable dates to aid troubleshooting, but doesn't fail
     * the operation to maintain resilience against data quality issues.
     *
     * @param artifact JsonNode containing the artifact data
     * @param cutoff   The cutoff date to compare against
     * @return true if uploadDate >= cutoff, false otherwise (including parse errors)
     */
    private static boolean isUploadDateOnOrAfter(JsonNode artifact, OffsetDateTime cutoff) {
        String uploadDateStr = artifact.path("uploadDate").asText("");
        if (uploadDateStr.isBlank()) { return false; }
        try {
            OffsetDateTime uploadDate = OffsetDateTime.parse(uploadDateStr);
            return !uploadDate.isBefore(cutoff);
        } catch (Exception e) {
            // Log the issue but don't fail - this allows processing to continue
            // even if some artifacts have malformed dates
            String artifactId = artifact.path("id").asText("unknown");
            LOG.warn("Unable to parse uploadDate '{}' for artifact {}: {}. Artifact will be excluded from results.",
                    uploadDateStr, artifactId, e.getMessage());
            return false;
        }
    }

    private static String buildNoArtifactsMessage(String appVersionId, OffsetDateTime sinceDate) {
        String base = "No Aviator-processed artifacts found for application version ID: " + appVersionId;
        if (sinceDate != null) {
            return base +
                   " uploaded on or after " + sinceDate;
        }
        return base;
    }

    private static JsonNode getArtifactJsonNode(UnirestInstance unirest, String artifactId) {
        return unirest.get(SSCUrls.ARTIFACT(artifactId))
                .queryString("embed","scans")
                .asObject(JsonNode.class).getBody().get("data");
    }

    public static final SSCArtifactDescriptor delete(UnirestInstance unirest, SSCArtifactDescriptor descriptor) {
        unirest.delete(SSCUrls.ARTIFACT(descriptor.getId())).asObject(JsonNode.class).getBody();
        return descriptor;
    }

    public static final SSCArtifactDescriptor purge(UnirestInstance unirest, SSCArtifactDescriptor descriptor) {
        unirest.post(SSCUrls.ARTIFACTS_ACTION_PURGE)
            .body(new SSCAppVersionArtifactPurgeByIdRequest(new String[] {descriptor.getId()}))
            .asObject(JsonNode.class).getBody();
        return descriptor;
    }

    public static final JsonNode purge(UnirestInstance unirest, SSCAppVersionArtifactPurgeByDateRequest purgeRequest) {
        return unirest.post(SSCUrls.PROJECT_VERSIONS_ACTION_PURGE)
                .body(purgeRequest).asObject(JsonNode.class).getBody();
    }

    public static final JsonNode approve(UnirestInstance unirest, String artifactId, String message){
        int[] artifactIds = {Integer.parseInt(artifactId)};

        JsonNode jsonNode = new ObjectMapper().createObjectNode()
                .putPOJO("artifactIds", artifactIds)
                .put("comment", message);

        return unirest.post(SSCUrls.ARTIFACTS_ACTION_APPROVE)
                .body(jsonNode)
                .asObject(JsonNode.class).getBody();
    }

    public static final String getArtifactStatus(UnirestInstance unirest, String artifactId){
        return JsonHelper.evaluateSpelExpression(
                unirest.get(SSCUrls.ARTIFACT(artifactId)).asObject(JsonNode.class).getBody(),
                "data.status",
                String.class
        );
    }

    @Data
    @Reflectable @NoArgsConstructor @AllArgsConstructor
    private static final class SSCAppVersionArtifactPurgeByIdRequest {
        private String[] artifactIds;
    }

    @Data @Builder
    @Reflectable @NoArgsConstructor @AllArgsConstructor
    public static final class SSCAppVersionArtifactPurgeByDateRequest {
        private String[] projectVersionIds;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSxxx")
        private OffsetDateTime purgeBefore;
    }

    private static final SSCArtifactDescriptor getDescriptor(JsonNode scanNode) {
        return JsonHelper.treeToValue(scanNode, SSCArtifactDescriptor.class);
    }

    public static JsonNode addScanTypes(JsonNode record) {
        if ( record instanceof ObjectNode && record.has("_embed") ) {
            JsonNode _embed = record.get("_embed");
            String scanTypesString = "";
            if ( _embed.has("scans") ) {
                // TODO Can we get rid of unchecked conversion warning?
                @SuppressWarnings("unchecked")
                ArrayList<String> scanTypes = JsonHelper.evaluateSpelExpression(_embed, "scans?.![type]", ArrayList.class);
                scanTypesString = scanTypes.stream().collect(Collectors.joining(", "));
            }
            record = ((ObjectNode)record).put("scanTypes", scanTypesString);
        }
        return record;
    }
}
