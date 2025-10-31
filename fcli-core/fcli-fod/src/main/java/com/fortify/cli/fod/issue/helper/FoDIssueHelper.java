/*
 * Copyright 2021-2025 Open Text.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;

import kong.unirest.UnirestInstance;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

public class FoDIssueHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

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

}