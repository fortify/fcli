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
package com.fortify.cli.license.ncd_report.descriptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;

import lombok.Data;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({
    "reportType",
    "reportDate",
    "reportStartDate",
    "reportEndDate",
    "generatedBy",
    "repositoryCounts",
    "commitCount",
    "authorCount",
    "detailRowCount",
    "mergedReportCount",
    "mergedSourceReports",
    "mergedDuplicateExpression",
    "logCounts"
})
public class NcdReportSummaryDescriptor {
    private String reportType;
    private String reportDate;
    private String reportStartDate;
    private String reportEndDate;
    private String generatedBy;
    private RepositoryCounts repositoryCounts;
    private CommitCount commitCount;
    private AuthorCount authorCount;
    private DetailRowCount detailRowCount;
    private Integer mergedReportCount;
    private List<String> mergedSourceReports;
    private String mergedDuplicateExpression;
    private LogCounts logCounts;

    private final Map<String, JsonNode> additionalProperties = new LinkedHashMap<>();

    public static NcdReportSummaryDescriptor fromObjectNode(ObjectNode node) {
        if ( node == null ) {
            return new NcdReportSummaryDescriptor();
        }
        return JsonHelper.treeToValue(node, NcdReportSummaryDescriptor.class);
    }

    public ObjectNode toObjectNode() {
        return JsonHelper.getObjectMapper().valueToTree(this);
    }

    public void applyTo(ObjectNode targetNode) {
        targetNode.removeAll();
        targetNode.setAll(toObjectNode());
    }

    @JsonAnySetter
    public void setAdditionalProperty(String propertyName, JsonNode propertyValue) {
        additionalProperties.put(propertyName, propertyValue);
    }

    @JsonAnyGetter
    public Map<String, JsonNode> getAdditionalProperties() {
        return additionalProperties;
    }

    @Reflectable @NoArgsConstructor
    @Data
    @JsonPropertyOrder({"total", "included", "excluded", "empty", "error", "dormant"})
    public static class RepositoryCounts {
        private Integer total;
        private Integer included;
        private Integer excluded;
        private Integer empty;
        private Integer error;
        private Integer dormant;
    }

    @Reflectable @NoArgsConstructor
    @Data
    @JsonPropertyOrder({"analyzed"})
    public static class CommitCount {
        private Integer analyzed;
    }

    @Reflectable @NoArgsConstructor
    @Data
    @JsonPropertyOrder({"total", "contributing", "ignored", "nonIgnored", "duplicate", "dormant"})
    public static class AuthorCount {
        private Integer total;
        private Integer contributing;
        private Integer ignored;
        private Integer nonIgnored;
        private Integer duplicate;
        private Integer dormant;
    }

    @Reflectable @NoArgsConstructor
    @Data
    @JsonPropertyOrder({"repositories", "commitsByBranch", "commitsByRepository", "contributorsByRepository"})
    public static class DetailRowCount {
        private Integer repositories;
        private Integer commitsByBranch;
        private Integer commitsByRepository;
        private Integer contributorsByRepository;
    }

    @Reflectable @NoArgsConstructor
    @Data
    @JsonPropertyOrder({"error", "warn"})
    public static class LogCounts {
        private Integer error;
        private Integer warn;
    }
}