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
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.embed.IFoDEntityEmbedder;
import com.fortify.cli.fod._common.rest.helper.FoDInputTransformer;
import com.fortify.cli.fod._common.rest.helper.FoDPagingHelper;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod.issue.cli.mixin.FoDIssueEmbedMixin;
import com.fortify.cli.fod.issue.cli.mixin.FoDIssueIncludeMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import io.micrometer.common.util.StringUtils;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDIssueHelper {
    private static final Log LOG = LogFactory.getLog(FoDIssueHelper.class);
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode transformRecord(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDBulkIssueUpdateResponse updateIssues(UnirestInstance unirest, String releaseId, FoDBulkIssueUpdateRequest issueUpdateRequest) {
        ObjectNode body = objectMapper.valueToTree(issueUpdateRequest);
        var result = unirest.post(FoDUrls.VULNERABILITIES + "/bulk-edit")
            .routeParam("relId", releaseId)
            .body(body).asObject(JsonNode.class).getBody();
        return getResponse(result);
    }

    public static final ArrayNode getReleaseIssues(UnirestInstance unirest,
                                                String releaseId,
                                                FoDIssueIncludeMixin includeMixin,
                                                FoDIssueEmbedMixin embedMixin,
                                                String filtersParamValue,
                                                boolean failOnError) {
        LOG.debug("Retrieving issues for release id: " + releaseId);
        FoDReleaseDescriptor releaseDescriptor = FoDReleaseHelper.getReleaseDescriptorFromId(unirest,
                Integer.parseInt(releaseId), true );
        String releaseName = releaseDescriptor.getReleaseName();
        ArrayNode result = JsonHelper.getObjectMapper().createArrayNode();
        Map<String, Object> queryParams = new HashMap<>();
        // Add filters parameter if specified
        if (StringUtils.isNotEmpty(filtersParamValue)) {
            queryParams.put("filters", filtersParamValue);
        }
        // Always order by severity ascending
        queryParams.put("orderBy", "severity");
        queryParams.put("orderDirection", "ASC");
        // Retrieve all issues for the specified release
        try {
            HttpRequest<?> request = unirest.get(FoDUrls.VULNERABILITIES)
                    .routeParam("relId", releaseId)
                    .queryString(queryParams);
            if (includeMixin != null) {
                request = includeMixin.updateRequest(request);
            }
            List<JsonNode> results = FoDPagingHelper.pagedRequest(request)
                    .stream()
                    .map(HttpResponse::getBody)
                    .map(FoDInputTransformer::getItems)
                    .map(ArrayNode.class::cast)
                    .flatMap(JsonHelper::stream)
                    .collect(Collectors.toList());
            // Transform and enhance each issue record
            for (JsonNode record : results) {
                if (record instanceof ObjectNode) {
                    transformRecord(record);
                    // add releaseName as only releaseId is returned in issue records
                    ((ObjectNode) record).put("releaseName", releaseName);
                    // add issueUrl for convenience
                    ((ObjectNode) record).put("issueUrl", getIssueUrl(unirest, record.get("id").asText()));
                    // embed additional entities if requested
                    if (embedMixin != null && embedMixin.getEmbedSuppliers() != null) {
                        for (FoDIssueEmbedMixin.FoDIssueEmbedderSupplier supplier : embedMixin.getEmbedSuppliers()) {
                            IFoDEntityEmbedder embedder = supplier.createEntityEmbedder();
                            embedder.embed(unirest, (ObjectNode) record);
                        }
                    }
                }
                result.add(record);
            }
            return result;
        } catch (UnexpectedHttpResponseException e) {
            if (failOnError) {
                throw e;
            }
            LOG.error("Error retrieving issues for release " +
                    (StringUtils.isNotEmpty(releaseId) ? releaseId : "<null>") +
                    ": " + e.getMessage());
            return JsonHelper.getObjectMapper().createArrayNode();
        }
    }

    public static final ArrayNode mergeReleaseIssues(ArrayNode issues) {
        Map<String, ObjectNode> merged = new HashMap<>();
        Map<String, Set<String>> releaseNamesByInstance = new HashMap<>();
        Map<String, Set<String>> releaseIdsByInstance = new HashMap<>();
        Map<String, Set<String>> idsByInstance = new HashMap<>();
        Map<String, Set<String>> vulnIdsByInstance = new HashMap<>();

        // Combine releaseId, releaseName, id, and vulnId fields for issues with the same instanceId
        for (JsonNode record : issues) {
            if (record instanceof ObjectNode) {
                String instanceId = record.get("instanceId").asText();
                String releaseNameVal = record.get("releaseName").asText();
                String releaseIdVal = record.get("releaseId").asText();
                String idVal = record.get("id").asText();
                String vulnIdVal = record.has("vulnId") ? record.get("vulnId").asText() : "";

                releaseNamesByInstance.computeIfAbsent(instanceId, k -> new HashSet<>()).add(releaseNameVal);
                releaseIdsByInstance.computeIfAbsent(instanceId, k -> new HashSet<>()).add(releaseIdVal);
                idsByInstance.computeIfAbsent(instanceId, k -> new HashSet<>()).add(idVal);
                if (!vulnIdVal.isEmpty()) {
                    vulnIdsByInstance.computeIfAbsent(instanceId, k -> new HashSet<>()).add(vulnIdVal);
                }

                merged.putIfAbsent(instanceId, (ObjectNode) record.deepCopy());
            }
        }

        // Sort merged records by severity, then category, then releaseId
        List<ObjectNode> sortedRecords = new ArrayList<>(merged.values());
        sortedRecords.sort(
                Comparator.comparingInt((ObjectNode n) -> n.get("severity").asInt()).reversed()
                        .thenComparing(n -> n.get("category").asText())
                        .thenComparing(n -> n.get("releaseId").asInt())
        );

        // Add combined fields to each record
        ArrayNode result = JsonHelper.getObjectMapper().createArrayNode();
        for (ObjectNode record : sortedRecords) {
            String instanceId = record.get("instanceId").asText();
            Set<String> releaseNames = releaseNamesByInstance.get(instanceId);
            Set<String> releaseIds = releaseIdsByInstance.get(instanceId);
            Set<String> relatedIds = idsByInstance.get(instanceId);
            Set<String> vulnIds = vulnIdsByInstance.getOrDefault(instanceId, Collections.emptySet());
            ArrayNode vulnIdsArray = JsonHelper.getObjectMapper().createArrayNode();
            vulnIds.forEach(vulnIdsArray::add);
            ArrayNode releaseNamesArray = JsonHelper.getObjectMapper().createArrayNode();
            releaseNames.forEach(releaseNamesArray::add);
            ArrayNode releaseIdsArray = JsonHelper.getObjectMapper().createArrayNode();
            releaseIds.forEach(releaseIdsArray::add);
            ArrayNode idsArray = JsonHelper.getObjectMapper().createArrayNode();
            relatedIds.forEach(idsArray::add);
            record.set("vulnIds", vulnIdsArray);
            record.put("vulnIdsString", String.join(", ", vulnIds));
            record.set("foundInReleases", releaseNamesArray);
            record.put("foundInReleasesString", String.join(", ", releaseNames));
            record.set("foundInReleaseIds", releaseIdsArray);
            record.put("foundInReleaseIdsString", String.join(", ", releaseIds));
            record.set("ids", idsArray);
            record.put("idsString", relatedIds.stream()
                    .map(Integer::parseInt)
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ")));
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