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
package com.fortify.cli.license.ncd_report.helper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.license.ncd_report.reader.NcdReportReader;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class NcdReportRepositoriesOutputHelper {
    private final NcdReportReader reader;
    private List<String> cachedEntries;
    private Map<String, RepositoryRawCounts> cachedRawCountsByRepositoryKey;
    private Boolean cachedHasLegacyRepositoryDetails;
    private Boolean cachedHasCommitDetails;
    private Boolean cachedHasContributorDetails;

    public Stream<ObjectNode> readRepositoriesAsOutputRows() {
        var hasLegacyRepositoryDetails = hasLegacyRepositoryDetails();
        var rawCountsByRepositoryKey = hasLegacyRepositoryDetails
                ? getRawCountsByRepositoryKey()
                : Map.<String, RepositoryRawCounts>of();
        return reader.readRepositoriesAsObjectNodeStream().map(row -> {
            var repositoryUrl = row.path("repositoryUrl").asText("");
            var sourceReport = row.path("sourceReport").asText("");
            var commitCountRaw = normalizeRawCountValue(row.path("commitCountRaw").asText(""));
            var contributorCountRaw = normalizeRawCountValue(row.path("contributorCountRaw").asText(""));
            if ( hasLegacyRepositoryDetails ) {
                var repositoryKey = repositoryKey(repositoryUrl, sourceReport);
                var rawCounts = rawCountsByRepositoryKey.get(repositoryKey);
                commitCountRaw = hasCommitDetails()
                        ? String.valueOf(rawCounts == null ? 0 : rawCounts.commitCountRaw())
                        : "unknown";
                contributorCountRaw = hasContributorDetails()
                        ? String.valueOf(rawCounts == null ? 0 : rawCounts.contributorCountRaw())
                        : "unknown";
            }
            return formatRepositoryRow(
                    repositoryUrl,
                    row.path("repositoryName").asText(""),
                    row.path("visibility").asText(""),
                    row.path("fork").asText(""),
                    row.path("status").asText(""),
                    row.path("reason").asText(""),
                    normalizeDormant(row.path("dormant").asText("")),
                    normalizeRawCountValue(commitCountRaw),
                    normalizeRawCountValue(contributorCountRaw),
                    sourceReport);
        });
    }

    private boolean hasLegacyRepositoryDetails() {
        if ( cachedHasLegacyRepositoryDetails == null ) {
            cachedHasLegacyRepositoryDetails = reader.hasLegacyRepositoryDetails();
        }
        return cachedHasLegacyRepositoryDetails;
    }

    private boolean hasCommitDetails() {
        if ( cachedHasCommitDetails == null ) {
            cachedHasCommitDetails = hasLegacyRepositoryDetails()
                    && hasEntry("details/commits-by-repository.csv");
        }
        return cachedHasCommitDetails;
    }

    private boolean hasContributorDetails() {
        if ( cachedHasContributorDetails == null ) {
            cachedHasContributorDetails = hasLegacyRepositoryDetails()
                    && hasEntry("details/contributors-by-repository.csv");
        }
        return cachedHasContributorDetails;
    }

    private boolean hasEntry(String entryName) {
        if ( cachedEntries == null ) {
            cachedEntries = reader.listFileEntries();
        }
        return cachedEntries.contains(entryName);
    }

    private Map<String, RepositoryRawCounts> getRawCountsByRepositoryKey() {
        if ( cachedRawCountsByRepositoryKey == null ) {
            cachedRawCountsByRepositoryKey = computeRawCounts();
        }
        return cachedRawCountsByRepositoryKey;
    }

    private Map<String, RepositoryRawCounts> computeRawCounts() {
        var commitCountsByRepositoryKey = new HashMap<String, Integer>();
        var contributorIdsByRepositoryKey = new HashMap<String, Set<String>>();

        if ( hasCommitDetails() ) {
            try ( var commits = reader.readCommitsByRepositoryAsObjectNodeStream() ) {
                commits.forEach(row -> {
                    var repositoryKey = repositoryKey(row.path("repositoryUrl").asText(""), row.path("sourceReport").asText(""));
                    if ( StringUtils.isBlank(repositoryKey) ) {
                        return;
                    }
                    commitCountsByRepositoryKey.put(repositoryKey, commitCountsByRepositoryKey.getOrDefault(repositoryKey, 0) + 1);
                });
            }
        }

        if ( hasContributorDetails() ) {
            try ( var contributors = reader.readContributorsByRepositoryAsObjectNodeStream() ) {
                contributors.forEach(row -> {
                    var repositoryKey = repositoryKey(row.path("repositoryUrl").asText(""), row.path("sourceReport").asText(""));
                    if ( StringUtils.isBlank(repositoryKey) ) {
                        return;
                    }
                    var authorId = row.path("authorId").asText("");
                    if ( StringUtils.isBlank(authorId) ) {
                        var expressionInput = NcdReportContributorHelper.createExpressionInput(
                                row.path("authorName").asText(""),
                                row.path("authorEmail").asText(""));
                        authorId = NcdReportContributorHelper.computeAuthorId(expressionInput);
                    }
                    contributorIdsByRepositoryKey.computeIfAbsent(repositoryKey, k -> new HashSet<>()).add(authorId);
                });
            }
        }

        var result = new HashMap<String, RepositoryRawCounts>();
        var repositoryKeys = new HashSet<String>();
        repositoryKeys.addAll(commitCountsByRepositoryKey.keySet());
        repositoryKeys.addAll(contributorIdsByRepositoryKey.keySet());
        repositoryKeys.forEach(repositoryKey -> result.put(repositoryKey,
                new RepositoryRawCounts(
                        commitCountsByRepositoryKey.getOrDefault(repositoryKey, 0),
                        contributorIdsByRepositoryKey.getOrDefault(repositoryKey, Set.of()).size())));
        return result;
    }

    private ObjectNode formatRepositoryRow(String repositoryUrl, String repositoryName, String visibility, String fork, String status,
            String reason, String dormant, String commitCountRaw, String contributorCountRaw, String sourceReport)
    {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("repositoryUrl", StringUtils.defaultString(repositoryUrl))
                .put("repositoryName", StringUtils.defaultString(repositoryName))
                .put("visibility", StringUtils.defaultString(visibility))
                .put("fork", StringUtils.defaultString(fork))
                .put("status", StringUtils.defaultString(status))
                .put("reason", StringUtils.defaultString(reason))
                .put("dormant", normalizeDormant(dormant))
                .put("commitCountRaw", normalizeRawCountValue(commitCountRaw))
                .put("contributorCountRaw", normalizeRawCountValue(contributorCountRaw))
                .put("sourceReport", StringUtils.defaultString(sourceReport));
    }

    private String normalizeDormant(String dormant) {
        var normalized = StringUtils.defaultString(dormant).trim();
        return StringUtils.isBlank(normalized) ? "unknown" : normalized;
    }

    private String normalizeRawCountValue(String count) {
        var normalized = StringUtils.defaultString(count).trim();
        return StringUtils.isBlank(normalized) ? "unknown" : normalized;
    }

    private String repositoryKey(String repositoryUrl, String sourceReport) {
        var normalizedRepositoryUrl = StringUtils.defaultString(repositoryUrl);
        if ( StringUtils.isBlank(normalizedRepositoryUrl) ) {
            return "";
        }
        return normalizedRepositoryUrl + "|" + StringUtils.defaultString(sourceReport);
    }

    private static record RepositoryRawCounts(
            int commitCountRaw,
            int contributorCountRaw
    ) {}
}