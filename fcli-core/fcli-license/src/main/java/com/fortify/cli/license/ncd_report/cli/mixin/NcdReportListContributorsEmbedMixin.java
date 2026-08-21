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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.license.ncd_report.helper.NcdReportContributorHelper;
import com.fortify.cli.license.ncd_report.helper.NcdReportRepositoriesOutputHelper;
import com.fortify.cli.license.ncd_report.reader.NcdReportReader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Option;

public class NcdReportListContributorsEmbedMixin {
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names = "--embed", required = false, split = ",")
    @Getter private EmbedOption[] embedOptions;

    private NcdReportReader embedHelperReader;
    private NcdReportContributorsEmbedHelper embedHelper;

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

    private NcdReportContributorsEmbedHelper getEmbedHelper(NcdReportReader reader) {
        if ( embedHelper == null || embedHelperReader != reader ) {
            embedHelperReader = reader;
            embedHelper = new NcdReportContributorsEmbedHelper(reader);
        }
        return embedHelper;
    }

    public boolean hasEmbedOption(EmbedOption embedOption) {
        if ( embedOptions == null || embedOptions.length == 0 ) {
            return false;
        }
        for ( var option : embedOptions ) {
            if ( option == embedOption ) {
                return true;
            }
        }
        return false;
    }

    @RequiredArgsConstructor
    public static enum EmbedOption {
        repositories(EmbedOption::enrichRepositories);

        private final BiConsumer<ObjectNode, NcdReportContributorsEmbedHelper> enricher;

        public void enrich(ObjectNode record, NcdReportContributorsEmbedHelper helper) {
            enricher.accept(record, helper);
        }

        private static void enrichRepositories(ObjectNode record, NcdReportContributorsEmbedHelper helper) {
            var authorId = record.path("authorId").asText("");
            record.set("repositories", helper.repositoriesForAuthor(authorId));
        }
    }

    @RequiredArgsConstructor
    private static final class NcdReportContributorsEmbedHelper {
        private final NcdReportReader reader;
        private Map<String, LinkedHashMap<String, ObjectNode>> repositoriesByAuthorId;

        public ArrayNode repositoriesForAuthor(String authorId) {
            if ( StringUtils.isBlank(authorId) ) {
                return JsonHelper.getObjectMapper().createArrayNode();
            }
            loadRepositoriesByAuthorIdIfNeeded();
            return repositoriesByAuthorId
                .getOrDefault(authorId, new LinkedHashMap<>())
                .values()
                .stream()
                .map(ObjectNode::deepCopy)
                .collect(JsonHelper.arrayNodeCollector());
        }

        private void loadRepositoriesByAuthorIdIfNeeded() {
            if ( repositoriesByAuthorId != null ) {
                return;
            }

            var repositoriesByKey = new LinkedHashMap<String, ObjectNode>();
            try ( var repositories = new NcdReportRepositoriesOutputHelper(reader).readRepositoriesAsOutputRows() ) {
                repositories.forEach(row -> repositoriesByKey.put(repositoryKey(row), row));
            }

            var result = new LinkedHashMap<String, LinkedHashMap<String, ObjectNode>>();
            try ( var contributorsByRepository = reader.readContributorsByRepositoryAsObjectNodeStream() ) {
                contributorsByRepository.forEach(row -> {
                    var repositoryKey = repositoryKey(row.path("repositoryUrl").asText(""), row.path("sourceReport").asText(""));
                    if ( StringUtils.isBlank(repositoryKey) ) {
                        return;
                    }
                    var repositoryRow = repositoriesByKey.get(repositoryKey);
                    if ( repositoryRow == null ) {
                        return;
                    }
                    var authorId = resolveAuthorId(row);
                    if ( StringUtils.isBlank(authorId) ) {
                        return;
                    }
                    result.computeIfAbsent(authorId, k -> new LinkedHashMap<>()).putIfAbsent(repositoryKey, repositoryRow);
                });
            }
            repositoriesByAuthorId = result;
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

        private String repositoryKey(ObjectNode row) {
            return repositoryKey(row.path("repositoryUrl").asText(""), row.path("sourceReport").asText(""));
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