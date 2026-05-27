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
package com.fortify.cli.fod.issue.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDInputTransformer;
import com.fortify.cli.fod._common.rest.helper.FoDPagingHelper;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;

import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.Builder;
import lombok.Data;

public class FoDIssueHelper {
    private static final Logger LOG = LoggerFactory.getLogger(FoDIssueHelper.class);

    public static final JsonNode transformRecord(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    /** Immutable aggregation data carrier for adding merged issue fields. */
    @Data @Builder
    public static class IssueAggregationData {
        private final Set<String> releaseNames;
        private final Set<String> releaseIds;
        private final Set<String> ids;
        private final Set<String> vulnIds;

        /** Factory for single-release records; extracts properties from issue node. */
        public static IssueAggregationData forSingleRelease(ObjectNode issue) {
            String releaseId = issue.get("releaseId").asText();
            String releaseName = issue.get("releaseName").asText();
            String id = issue.get("id").asText();
            String vulnId = issue.has("vulnId") ? issue.get("vulnId").asText() : null;
            return IssueAggregationData.builder()
                .releaseNames(Set.of(releaseName))
                .releaseIds(Set.of(releaseId))
                .ids(Set.of(id))
                .vulnIds(Set.of(vulnId))
                .build();
        }

        /** Factory returning an IssueAggregationData instance with all aggregation fields empty. */
        public static IssueAggregationData blank() {
            return IssueAggregationData.builder()
                .releaseNames(Collections.emptySet())
                .releaseIds(Collections.emptySet())
                .ids(Collections.emptySet())
                .vulnIds(Collections.emptySet())
                .build();
        }

        public String getVulnIdsString() {
            return asString(vulnIds);
        }

        public String getReleaseNamesString() {
            return asString(releaseNames);
        }

        public String getReleaseIdsString() {
            return asString(releaseIds);
        }

        public String getIdsString() {
        return asString(ids);
        }

        private String asString(Set<String> values) {
            return values==null || values.isEmpty()
                    ? "N/A"
                    : String.join(", ", values);
        }
    }

    /** Overload adding aggregation fields to an ObjectNode using provided data. */
    public static final ObjectNode transformRecord(ObjectNode record, IssueAggregationData data) {
        transformRecord(record); // apply generic transformations first (rename etc.)
        ArrayNode vulnIdsArray = JsonHelper.getObjectMapper().createArrayNode();
        data.getVulnIds().forEach(vulnIdsArray::add);
        ArrayNode releaseNamesArray = JsonHelper.getObjectMapper().createArrayNode();
        data.getReleaseNames().forEach(releaseNamesArray::add);
        ArrayNode releaseIdsArray = JsonHelper.getObjectMapper().createArrayNode();
        data.getReleaseIds().forEach(releaseIdsArray::add);
        ArrayNode idsArray = JsonHelper.getObjectMapper().createArrayNode();
        data.getIds().forEach(idsArray::add);
        record.set("vulnIds", vulnIdsArray);
        record.put("vulnIdsString", data.getVulnIdsString());
        record.set("foundInReleases", releaseNamesArray);
        record.put("foundInReleasesString", data.getReleaseNamesString());
        record.set("foundInReleaseIds", releaseIdsArray);
        record.put("foundInReleaseIdsString", data.getReleaseIdsString());
        record.set("ids", idsArray);
        record.put("idsString", data.getIdsString());
        return record;
    }

    public static final FoDBulkIssueUpdateResponse updateIssues(UnirestInstance unirest, String releaseId, FoDBulkIssueUpdateRequest issueUpdateRequest) {
        ObjectNode body = JsonHelper.getObjectMapper().valueToTree(issueUpdateRequest);
        var result = unirest.post(FoDUrls.VULNERABILITIES + "/bulk-edit")
            .routeParam("relId", releaseId)
            .body(body).asObject(JsonNode.class).getBody();
        return getResponse(result);
    }

    public static final ArrayNode mergeReleaseIssues(ArrayNode issues) {
        Map<String, ObjectNode> merged = new HashMap<>();
        Map<String, Set<String>> releaseNamesByInstance = new HashMap<>();
        Map<String, Set<String>> releaseIdsByInstance = new HashMap<>();
        Map<String, Set<String>> idsByInstance = new HashMap<>();
        Map<String, Set<String>> vulnIdsByInstance = new HashMap<>();

        for (JsonNode record : issues) {
            if (record instanceof ObjectNode objectNode) {
                String instanceId = objectNode.get("instanceId").asText();
                String releaseNameVal = objectNode.get("releaseName").asText();
                String releaseIdVal = objectNode.get("releaseId").asText();
                String idVal = objectNode.get("id").asText();
                String vulnIdVal = objectNode.has("vulnId") ? objectNode.get("vulnId").asText() : "";

                releaseNamesByInstance.computeIfAbsent(instanceId, k -> new HashSet<>()).add(releaseNameVal);
                releaseIdsByInstance.computeIfAbsent(instanceId, k -> new HashSet<>()).add(releaseIdVal);
                idsByInstance.computeIfAbsent(instanceId, k -> new HashSet<>()).add(idVal);
                if (!vulnIdVal.isEmpty()) {
                    vulnIdsByInstance.computeIfAbsent(instanceId, k -> new HashSet<>()).add(vulnIdVal);
                }

                merged.putIfAbsent(instanceId, (ObjectNode) objectNode.deepCopy());
            }
        }

        List<ObjectNode> sortedRecords = new ArrayList<>(merged.values());
        sortedRecords.sort(
                Comparator.comparingInt((ObjectNode n) -> n.get("severity").asInt()).reversed()
                        .thenComparing(n -> n.get("category").asText())
                        .thenComparing(n -> n.get("releaseId").asInt())
        );

        ArrayNode result = JsonHelper.getObjectMapper().createArrayNode();
        for (ObjectNode record : sortedRecords) {
            String instanceId = record.get("instanceId").asText();
            Set<String> releaseNames = releaseNamesByInstance.get(instanceId);
            Set<String> releaseIds = releaseIdsByInstance.get(instanceId);
            Set<String> issueIds = idsByInstance.get(instanceId);
            Set<String> vulnIds = vulnIdsByInstance.getOrDefault(instanceId, Collections.emptySet());
            var data = IssueAggregationData.builder()
                    .releaseNames(releaseNames)
                    .releaseIds(releaseIds)
                    .ids(issueIds)
                    .vulnIds(vulnIds)
                    .build();
            transformRecord(record, data);
            result.add(record);
        }
        return result;
    }


    public static final String getIssueUrl(UnirestInstance unirestInstance, String issueId) {
        return String.format("%s/redirect/Issues/%s", getBaseUrl(unirestInstance), issueId);
    }


    private static final FoDBulkIssueUpdateResponse getResponse(JsonNode node) {
        return node==null ? null : JsonHelper.treeToValue(node, FoDBulkIssueUpdateResponse.class);
    }

    private static String getBaseUrl(UnirestInstance unirest) {
        return FoDProductHelper.INSTANCE.getBrowserUrl(unirest.config().getDefaultBaseUrl());
    }

    /**
     * Check whether an issue (vulnerability) exists in the given release.
     * Uses the vulnerabilities/{vulnId}/summary endpoint which returns 200 when present and 404 when not.
     * Returns true when the issue exists, false when a 404 is returned. Other HTTP errors are wrapped
     * in a FcliTechnicalException.
     */
    public static boolean issueExists(UnirestInstance unirest, String releaseId, String vulnId) {
        try {
            var response = unirest.get("/api/v3/releases/{releaseId}/vulnerabilities/{vulnId}/summary")
                    .routeParam("releaseId", releaseId)
                    .routeParam("vulnId", vulnId)
                    .asObject(JsonNode.class);
            int status = response.getStatus();
            if ( status==200 ) {
                return true;
            } else if ( status==404 ) {
                return false;
            } else {
                throw new FcliTechnicalException(String.format("Unexpected response checking issue existence: HTTP %d", status));
            }
        } catch (kong.unirest.UnirestException e) {
            throw new FcliTechnicalException("Error checking issue existence", e);
        }
    }

    /**
     * Bulk retrieval of vulnerability ids for a release. Returns a set containing the 'id' and 'vulnId'
     * string values for vulnerabilities in the release. If {@code requestedIds} is non-null and non-empty,
     * this method will stop paging early once all requested ids have been found (matching either id or vulnId).
     *
     * @param unirest Unirest instance
     * @param releaseId Release id
     * @param requestedIds Optional set of requested ids (either internal id or vulnId) to look for; may be null to fetch all
     * @return Set of found id/vulnId strings (trimmed)
     */
    public static java.util.Set<String> getVulnIdsForRelease(UnirestInstance unirest, String releaseId, java.util.Set<String> requestedIds) {
        var result = new java.util.HashSet<String>();
        // If a requested set is provided, track remaining items to allow early exit
        java.util.Set<String> remaining = null;
        if ( requestedIds!=null && !requestedIds.isEmpty() ) {
            remaining = new java.util.HashSet<>();
            for ( var s : requestedIds ) { if ( s!=null ) remaining.add(s.trim()); }
            // If after trimming nothing remains, treat as null
            if ( remaining.isEmpty() ) remaining = null;
        }
        try {
            var request = unirest.get(FoDUrls.VULNERABILITIES)
                    .routeParam("relId", releaseId)
                    .queryString("fields", "id,vulnId")
                    .queryString("includeFixed", "true")
                    .queryString("includeSuppressed", "true");
            var stream = FoDPagingHelper.pagedRequest(request).stream()
                .map(HttpResponse::getBody)
                .map(FoDInputTransformer::getItems)
                .filter(items -> items!=null && items.isArray())
                .map(ArrayNode.class::cast)
                .flatMap(JsonHelper::stream);

            for ( JsonNode item : (Iterable<JsonNode>)stream::iterator ) {
                if ( item.has("id") && !item.get("id").isNull() ) {
                    String id = item.get("id").asText().trim();
                    result.add(id);
                    if ( remaining!=null ) remaining.remove(id);
                }
                if ( item.has("vulnId") && !item.get("vulnId").isNull() ) {
                    String vid = item.get("vulnId").asText().trim();
                    result.add(vid);
                    if ( remaining!=null ) remaining.remove(vid);
                }
                if ( remaining!=null && remaining.isEmpty() ) {
                    // Found all requested ids; stop paging early
                    break;
                }
            }
        } catch (Exception e) {
            throw new FcliTechnicalException("Error retrieving vulnerabilities for release", e);
        }
        return result;
    }

    /**
     * Returns the numeric {@code id} for every vulnerability in the release, with no duplicates.
     * Unlike {@link #getVulnIdsForRelease}, which collects both {@code id} and {@code vulnId} per
     * item (needed for user-supplied identifier matching), this method collects only the canonical
     * numeric id so that the result count matches the actual vulnerability count and the bulk-edit
     * API is not sent duplicate identifiers.
     */
    public static List<String> getAllVulnNumericIdsForRelease(UnirestInstance unirest, String releaseId) {
        var result = new ArrayList<String>();
        try {
            var request = unirest.get(FoDUrls.VULNERABILITIES)
                    .routeParam("relId", releaseId)
                    .queryString("fields", "id")
                    .queryString("includeFixed", "true")
                    .queryString("includeSuppressed", "true");
            var stream = FoDPagingHelper.pagedRequest(request).stream()
                .map(HttpResponse::getBody)
                .map(FoDInputTransformer::getItems)
                .filter(items -> items != null && items.isArray())
                .map(ArrayNode.class::cast)
                .flatMap(JsonHelper::stream);
            for ( JsonNode item : (Iterable<JsonNode>)stream::iterator ) {
                if ( item.has("id") && !item.get("id").isNull() ) {
                    result.add(item.get("id").asText().trim());
                }
            }
        } catch (Exception e) {
            throw new FcliTechnicalException("Error retrieving vulnerabilities for release", e);
        }
        return result;
    }

    /**
     * Result carrier for vuln filtering: kept (normalized ids to update), skipped (original values skipped), totalCount
     */
    public static record VulnFilterResult(List<String> kept, List<String> skipped, int totalCount) {}

    /**
     * Filter a list of requested vuln identifiers (either internal 'id' or 'vulnId') against the release's
     * vulnerabilities. Preserves original order in the returned 'kept' list. Uses server-side paging and
     * early-exit when possible.
     */
    public static VulnFilterResult filterRequestedVulnIds(UnirestInstance unirest, String releaseId, List<String> requested) {
        int totalCount = requested==null ? 0 : requested.size();
        if ( requested==null || requested.isEmpty() ) {
            return new VulnFilterResult(new ArrayList<>(), new ArrayList<>(), totalCount);
        }
        // Build normalized requested set for lookup/early-exit
        var requestedSet = new HashSet<String>();
        for ( String vid : requested ) {
            String normalized = normalizeVulnId(vid);
            if ( normalized!=null ) { requestedSet.add(normalized); }
        }
         // Fetch ids (early-exit aware)
         var found = getVulnIdsForRelease(unirest, releaseId, requestedSet);
         var kept = new ArrayList<String>();
         var skipped = new ArrayList<String>();
        for ( String vid : requested ) {
            String normalized = normalizeVulnId(vid);
            if ( normalized!=null && found.contains(normalized) ) {
                kept.add(normalized);
            } else {
                skipped.add(vid);
            }
        }
        return new VulnFilterResult(kept, skipped, totalCount);
    }

    /**
     * Normalize a vulnerability identifier supplied by the user: trim and strip surrounding single or double quotes.
     */
    private static String normalizeVulnId(String id) {
        if ( id==null ) return null;
        String v = id.trim();
        if ( v.length() >= 2 ) {
            char f = v.charAt(0);
            char l = v.charAt(v.length()-1);
            if ((f=='\'' && l=='\'') || (f=='"' && l=='"')) {
                v = v.substring(1, v.length()-1).trim();
            }
        }
        return v.isEmpty() ? null : v;
    }
}
