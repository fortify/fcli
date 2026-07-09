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
package com.fortify.cli.license.ncd_report.cli.mixin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.license.ncd_report.helper.NcdReportContributorHelper;
import com.fortify.cli.license.ncd_report.helper.NcdReportContributorsOutputHelper;
import com.fortify.cli.license.ncd_report.reader.NcdReportReader;
import com.fortify.cli.license.ncd_report.writer.NcdReportContributorsCsvSchema;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Option;

public class NcdReportListRepositoriesEmbedMixin {
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names = "--embed", required = false, split = ",")
    @Getter private EmbedOption[] embedOptions;

    private NcdReportReader embedHelperReader;
    private NcdReportRepositoriesEmbedHelper embedHelper;

    public JsonNode enrichRecord(JsonNode record, NcdReportReader reader) {
        if ( embedOptions == null || embedOptions.length == 0 || !(record instanceof ObjectNode node) ) {
            return record;
        }
        var helper = getEmbedHelper(reader);
        for ( var embedOption : embedOptions ) {
            embedOption.enrich(node, helper);
        }
        return node;
    }

    private NcdReportRepositoriesEmbedHelper getEmbedHelper(NcdReportReader reader) {
        if ( embedHelper == null || embedHelperReader != reader ) {
            embedHelperReader = reader;
            embedHelper = new NcdReportRepositoriesEmbedHelper(reader);
        }
        return embedHelper;
    }

    public boolean hasEmbedOptions() {
        return embedOptions != null && embedOptions.length > 0;
    }

    @RequiredArgsConstructor
    public static enum EmbedOption {
        authors(EmbedOption::enrichAuthors),
        contributors(EmbedOption::enrichContributors);

        private final BiConsumer<ObjectNode, NcdReportRepositoriesEmbedHelper> enricher;

        public void enrich(ObjectNode record, NcdReportRepositoriesEmbedHelper helper) {
            enricher.accept(record, helper);
        }

        private static void enrichAuthors(ObjectNode record, NcdReportRepositoriesEmbedHelper helper) {
            var repositoryUrl = record.path("repositoryUrl").asText("");
            var sourceReport = record.path("sourceReport").asText("");
            record.set("authors", helper.authorsForRepository(repositoryUrl, sourceReport, false));
        }

        private static void enrichContributors(ObjectNode record, NcdReportRepositoriesEmbedHelper helper) {
            var repositoryUrl = record.path("repositoryUrl").asText("");
            var sourceReport = record.path("sourceReport").asText("");
            record.set("contributors", helper.authorsForRepository(repositoryUrl, sourceReport, true));
        }
    }

    @RequiredArgsConstructor
    private static final class NcdReportRepositoriesEmbedHelper {
        private final NcdReportReader reader;
        private List<ObjectNode> orderedAuthorRows;
        private Map<String, Set<String>> authorIdsByRepositoryKey;

        public ArrayNode authorsForRepository(String repositoryUrl, String sourceReport, boolean contributingOnly) {
            loadAuthorRowsAndRepositoryIndexIfNeeded();
            var repositoryKey = repositoryKey(repositoryUrl, sourceReport);
            var authorIds = authorIdsByRepositoryKey.get(repositoryKey);
            if ( authorIds == null || authorIds.isEmpty() ) {
                return JsonHelper.getObjectMapper().createArrayNode();
            }

            return orderedAuthorRows.stream()
                .filter(row -> authorIds.contains(row.path(NcdReportContributorsCsvSchema.AUTHOR_ID).asText("")))
                .filter(row -> !contributingOnly
                        || "contributing".equalsIgnoreCase(row.path(NcdReportContributorsCsvSchema.CONTRIBUTION_STATUS).asText("")))
                .map(ObjectNode::deepCopy)
                .collect(JsonHelper.arrayNodeCollector());
        }

        private void loadAuthorRowsAndRepositoryIndexIfNeeded() {
            if ( orderedAuthorRows != null && authorIdsByRepositoryKey != null ) {
                return;
            }

            try ( var authors = new NcdReportContributorsOutputHelper(reader).readContributorsAsOutputRows() ) {
                orderedAuthorRows = new ArrayList<>(authors.toList());
            }

            var result = new LinkedHashMap<String, Set<String>>();
            try ( var contributorsByRepository = reader.readContributorsByRepositoryAsObjectNodeStream() ) {
                contributorsByRepository.forEach(row -> {
                    var repositoryKey = repositoryKey(row.path("repositoryUrl").asText(""), row.path("sourceReport").asText(""));
                    if ( StringUtils.isBlank(repositoryKey) ) {
                        return;
                    }
                    var authorId = resolveAuthorId(row);
                    if ( StringUtils.isBlank(authorId) ) {
                        return;
                    }
                    result.computeIfAbsent(repositoryKey, k -> new LinkedHashSet<>()).add(authorId);
                });
            }
            authorIdsByRepositoryKey = result;
        }

        private String resolveAuthorId(ObjectNode row) {
            var authorId = row.path("authorId").asText("");
            if ( StringUtils.isNotBlank(authorId) ) {
                return authorId;
            }
            var expressionInput = NcdReportContributorHelper.createExpressionInput(
                    row.path("authorName").asText(""),
                    row.path("authorEmail").asText(""));
            return NcdReportContributorHelper.computeAuthorId(expressionInput);
        }

        private String repositoryKey(String repositoryUrl, String sourceReport) {
            var normalizedRepositoryUrl = StringUtils.defaultString(repositoryUrl);
            if ( StringUtils.isBlank(normalizedRepositoryUrl) ) {
                return "";
            }
            return normalizedRepositoryUrl + "|" + StringUtils.defaultString(sourceReport);
        }
    }
}