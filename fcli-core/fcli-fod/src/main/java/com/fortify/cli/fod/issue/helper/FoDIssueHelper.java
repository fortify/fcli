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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDInputTransformer;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDescriptor;
import com.fortify.cli.fod.attribute.helper.FoDAttributeHelper;

import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

public class FoDIssueHelper {
    private static final Logger LOG = LoggerFactory.getLogger(FoDIssueHelper.class);
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    // Local cache for attribute descriptors used during bulk issue updates. Populated by loadAllAttributes().
    private static final java.util.concurrent.ConcurrentHashMap<String, FoDAttributeDescriptor> ATTR_CACHE_BY_NAME = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<Integer, FoDAttributeDescriptor> ATTR_CACHE_BY_ID = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile boolean attributesPrefetched = false;

    /**
     * Prefetch all attributes from FoD and populate the local cache. Safe to call multiple times; will perform
     * the fetch only once per JVM unless clearAttributesCache() is called.
     */
    public static synchronized void loadAllAttributes(UnirestInstance unirest) {
        if ( attributesPrefetched ) return;
        // Use local temporary maps to build the cache so a failed fetch/parse doesn't leave partial data
        var tmpById = new java.util.HashMap<Integer, FoDAttributeDescriptor>();
        var tmpByName = new java.util.HashMap<String, FoDAttributeDescriptor>();
        try {
            var request = unirest.get(FoDUrls.ATTRIBUTES);
            var body = request.asObject(ObjectNode.class).getBody();
            var items = body.get("items");
            if ( items!=null && items.isArray() ) {
                for (var item : items) {
                    FoDAttributeDescriptor desc = JsonHelper.treeToValue(item, FoDAttributeDescriptor.class);
                    if ( desc!=null ) {
                        tmpById.putIfAbsent(desc.getId(), desc);
                        if ( desc.getName()!=null ) {
                            tmpByName.putIfAbsent(desc.getName(), desc);
                            tmpByName.putIfAbsent(desc.getName().trim(), desc);
                        }
                    }
                }
            }
            // Merge into the concurrent caches; preserve any existing descriptors using putIfAbsent
            for ( var e : tmpById.entrySet() ) {
                ATTR_CACHE_BY_ID.putIfAbsent(e.getKey(), e.getValue());
            }
            for ( var e : tmpByName.entrySet() ) {
                ATTR_CACHE_BY_NAME.putIfAbsent(e.getKey(), e.getValue());
            }
            attributesPrefetched = true; // set only after successful population
        } catch (kong.unirest.UnirestException e) {
            throw new com.fortify.cli.common.exception.FcliTechnicalException("Error loading attribute descriptors", e);
        } catch (Exception e) {
            throw new com.fortify.cli.common.exception.FcliTechnicalException("Error processing attribute descriptors", e);
        }
    }

    public static void clearAttributesCache() {
        ATTR_CACHE_BY_ID.clear();
        ATTR_CACHE_BY_NAME.clear();
        attributesPrefetched = false;
    }

    /**
     * Resolve an attribute descriptor from the local cache. If not prefetched yet, will call loadAllAttributes.
     */
    public static FoDAttributeDescriptor getAttributeDescriptorFromCache(UnirestInstance unirest, String nameOrId, boolean failIfNotFound) {
        if ( !attributesPrefetched ) {
            loadAllAttributes(unirest);
        }
        if ( nameOrId==null ) {
            if ( failIfNotFound ) throw new com.fortify.cli.common.exception.FcliSimpleException("No attribute found for name or id: null");
            return null;
        }
        try {
            int id = Integer.parseInt(nameOrId);
            var desc = ATTR_CACHE_BY_ID.get(id);
            if ( desc==null && failIfNotFound ) throw new com.fortify.cli.common.exception.FcliSimpleException("No attribute found for name or id: " + nameOrId);
            return desc;
        } catch (NumberFormatException nfe) {
            var desc = ATTR_CACHE_BY_NAME.get(nameOrId);
            if ( desc==null ) {
                // try trimmed
                desc = ATTR_CACHE_BY_NAME.get(nameOrId.trim());
            }
            if ( desc==null && failIfNotFound ) throw new com.fortify.cli.common.exception.FcliSimpleException("No attribute found for name or id: " + nameOrId);
            return desc;
        }
    }

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
        ObjectNode body = objectMapper.valueToTree(issueUpdateRequest);
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
                throw new com.fortify.cli.common.exception.FcliTechnicalException(String.format("Unexpected response checking issue existence: HTTP %d", status));
            }
        } catch (kong.unirest.UnirestException e) {
            throw new com.fortify.cli.common.exception.FcliTechnicalException("Error checking issue existence", e);
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
            var stream = com.fortify.cli.fod._common.rest.helper.FoDPagingHelper.pagedRequest(request).stream()
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
            throw new com.fortify.cli.common.exception.FcliTechnicalException("Error retrieving vulnerabilities for release", e);
        }
        return result;
    }

    /**
     * Resolve a status value (developer/auditor) against one or more FoD attribute picklists.
     * Returns the canonical picklist name when found, or throws a FcliSimpleException listing allowed values.
     */
    public static String resolveStatusValue(UnirestInstance unirest, String providedValue, String[] attributeNames, String optionName) {
        // Maintain compatibility by delegating to the generic overload; try to infer enum when optionName indicates developer/auditor
        FoDEnums.DeveloperStatusType[] devEnum = FoDEnums.DeveloperStatusType.values();
        FoDEnums.AuditorStatusType[] audEnum = FoDEnums.AuditorStatusType.values();
        if ( optionName!=null && optionName.toLowerCase().contains("developer") ) {
            return resolveStatusValue(unirest, providedValue, attributeNames, optionName, devEnum);
        } else if ( optionName!=null && optionName.toLowerCase().contains("auditor") ) {
            return resolveStatusValue(unirest, providedValue, attributeNames, optionName, audEnum);
        }
        return resolveStatusValue(unirest, providedValue, attributeNames, optionName, (FoDEnums.DeveloperStatusType[])null);
    }

    public static <T extends Enum<T> & FoDEnums.IFoDEnumValueSupplier<String>> String resolveStatusValue(UnirestInstance unirest, String providedValue, String[] attributeNames, String optionName, T[] enumValues) {
        if ( providedValue==null || providedValue.isBlank() ) { return null; }
        String originalProvided = providedValue;
        String candidate = providedValue.trim();
        try {
            if ( enumValues!=null ) {
                var resolved = FoDEnums.IFoDEnumValueSupplier.resolveEnumValue(candidate, enumValues);
                if ( resolved.isPresent() ) { candidate = resolved.get(); }
            }
        } catch (Exception e) {
            LOG.debug("Error resolving enum-style status value for {}: {}", optionName, e.getMessage());
        }

        String attrResolved = tryResolveAgainstAttributes(unirest, attributeNames, candidate);
        if ( attrResolved!=null ) return attrResolved;

        var allowed = collectAllowedAttributeValues(unirest, attributeNames);
        throw new FcliSimpleException(String.format("Invalid %s '%s'. Allowed values: %s", optionName, originalProvided, String.join(", ", allowed)));
    }

    private static String tryResolveAgainstAttributes(UnirestInstance unirest, String[] attributeNames, String candidate) {
        for (String attrName: attributeNames) {
            var desc = FoDAttributeHelper.getAttributeDescriptor(unirest, attrName, false);
            if ( desc==null ) continue;
            var picklist = desc.getPicklistValues();
            if ( picklist==null || picklist.isEmpty() ) continue;
            for (var pv : picklist) {
                if ( pv.getName()!=null && pv.getName().equalsIgnoreCase(candidate) ) {
                    return pv.getName();
                }
            }
            // if provided value looks like an id, try matching by id
            try {
                int providedId = Integer.parseInt(candidate);
                for (var pv : picklist) {
                    if ( java.util.Objects.equals(pv.getId(), providedId) ) {
                        return pv.getName();
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static List<String> collectAllowedAttributeValues(UnirestInstance unirest, String[] attributeNames) {
        var allowed = new ArrayList<String>();
        for (String attrName: attributeNames) {
            var desc = FoDAttributeHelper.getAttributeDescriptor(unirest, attrName, false);
            if ( desc==null ) continue;
            var picklist = desc.getPicklistValues();
            if ( picklist==null ) continue;
            for (var pv: picklist) {
                allowed.add(pv.getName());
            }
        }
        return allowed;
    }

    /**
     * Result carrier for vuln filtering: kept (normalized ids to update), skipped (original values skipped), totalCount
     */
    public static record VulnFilterResult(java.util.List<String> kept, java.util.List<String> skipped, int totalCount) {}

    /**
     * Filter a list of requested vuln identifiers (either internal 'id' or 'vulnId') against the release's
     * vulnerabilities. Preserves original order in the returned 'kept' list. Uses server-side paging and
     * early-exit when possible.
     */
    public static VulnFilterResult filterRequestedVulnIds(UnirestInstance unirest, String releaseId, java.util.List<String> requested) {
        int totalCount = requested==null ? 0 : requested.size();
        if ( requested==null || requested.isEmpty() ) {
            return new VulnFilterResult(new ArrayList<>(), new ArrayList<>(), totalCount);
        }
        // Build normalized requested set for lookup/early-exit
        var requestedSet = new java.util.HashSet<String>();
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

    /**
     * Build an ArrayNode of attribute objects (id/value) for Issue attribute updates using the localized
     * attribute cache. Will prefetch attributes if necessary.
     */
    public static ArrayNode buildIssueAttributesNode(UnirestInstance unirest, Map<String, String> attributeUpdates) {
        ArrayNode attrArray = JsonHelper.getObjectMapper().createArrayNode();
        if ( attributeUpdates==null || attributeUpdates.isEmpty() ) return attrArray;
        // Ensure local cache populated
        loadAllAttributes(unirest);
        for ( Map.Entry<String,String> e : attributeUpdates.entrySet() ) {
            String attrName = e.getKey();
            String value = e.getValue();
            FoDAttributeDescriptor attributeDescriptor = getAttributeDescriptorFromCache(unirest, attrName, true);
            // Only include attributes that are Issue-scoped (AttributeTypes.Issue)
            if ( attributeDescriptor!=null && attributeDescriptor.getAttributeTypeId() == FoDEnums.AttributeTypes.Issue.getValue() ) {
                var obj = JsonHelper.getObjectMapper().createObjectNode();
                obj.put("id", attributeDescriptor.getId());
                obj.put("value", value);
                attrArray.add(obj);
            } else {
                LOG.debug("Skipping attribute '{}' as it is not an Issue attribute", attributeDescriptor.getName());
            }
        }
        return attrArray;
    }
}
