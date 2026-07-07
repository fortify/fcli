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
package com.fortify.cli.license.ncd_report.cli.cmd;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.license.ncd_report.helper.NcdReportContributorHelper;
import com.fortify.cli.license.ncd_report.reader.NcdReportReader;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "list-repositories", aliases = {"lsr"})
public final class NcdReportListRepositoriesCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    private static final CsvMapper CSV_MAPPER = new CsvMapper();

    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;

    @Option(names = {"-r", "--report"}, required = true)
    @Getter private Path reportPath;

    @Override
    public JsonNode getJsonNode() {
        var result = JsonHelper.getObjectMapper().createArrayNode();
        try ( var reader = new NcdReportReader(reportPath) ) {
            var entries = reader.listFileEntries();
            var rows = entries.contains("repositories.csv")
                    ? readTopLevelRepositories(reader)
                    : readLegacyRepositories(reader, entries);
            rows.forEach(result::add);
        }
        return result;
    }

    @Override
    public boolean isSingular() {
        return false;
    }

    private List<ObjectNode> readTopLevelRepositories(NcdReportReader reader) {
        var rows = readCsvRows(reader, "repositories.csv");
        var result = new ArrayList<ObjectNode>(rows.size());
        for ( var row : rows ) {
            result.add(createOutputRow(
                    row.get("repositoryUrl"),
                    row.get("repositoryName"),
                    row.get("visibility"),
                    row.get("fork"),
                    row.get("status"),
                    row.get("reason"),
                    normalizeDormant(row.get("dormant")),
                    normalizeRawCountValue(row.get("commitCountRaw")),
                    normalizeRawCountValue(row.get("contributorCountRaw")),
                    row.get("sourceReport")));
        }
        return result;
    }

    private List<ObjectNode> readLegacyRepositories(NcdReportReader reader, List<String> entries) {
        var hasCommitDetails = entries.contains("details/commits-by-repository.csv");
        var hasContributorDetails = entries.contains("details/contributors-by-repository.csv");
        var rawCountsByRepositoryKey = computeRawCounts(reader, hasCommitDetails, hasContributorDetails);

        var rows = readCsvRows(reader, "details/repositories.csv");
        var result = new ArrayList<ObjectNode>(rows.size());
        for ( var row : rows ) {
            var repositoryKey = repositoryKey(row.get("repositoryUrl"), row.get("sourceReport"));
            var rawCounts = rawCountsByRepositoryKey.get(repositoryKey);
            var commitCountRaw = hasCommitDetails
                    ? String.valueOf(rawCounts == null ? 0 : rawCounts.commitCountRaw())
                    : "unknown";
            var contributorCountRaw = hasContributorDetails
                    ? String.valueOf(rawCounts == null ? 0 : rawCounts.contributorCountRaw())
                    : "unknown";

            result.add(createOutputRow(
                    row.get("repositoryUrl"),
                    row.get("repositoryName"),
                    row.get("visibility"),
                    row.get("fork"),
                    row.get("status"),
                    row.get("reason"),
                    normalizeDormant(row.get("dormant")),
                    commitCountRaw,
                    contributorCountRaw,
                    row.get("sourceReport")));
        }
        return result;
    }

    private Map<String, RepositoryRawCounts> computeRawCounts(NcdReportReader reader, boolean hasCommitDetails,
            boolean hasContributorDetails)
    {
        var commitCountsByRepositoryKey = new HashMap<String, Integer>();
        var contributorIdsByRepositoryKey = new HashMap<String, Set<String>>();

        if ( hasCommitDetails ) {
            for ( var row : readCsvRows(reader, "details/commits-by-repository.csv") ) {
                var repositoryKey = repositoryKey(row.get("repositoryUrl"), row.get("sourceReport"));
                if ( StringUtils.isBlank(repositoryKey) ) {
                    continue;
                }
                commitCountsByRepositoryKey.put(repositoryKey, commitCountsByRepositoryKey.getOrDefault(repositoryKey, 0) + 1);
            }
        }

        if ( hasContributorDetails ) {
            for ( var row : readCsvRows(reader, "details/contributors-by-repository.csv") ) {
                var repositoryKey = repositoryKey(row.get("repositoryUrl"), row.get("sourceReport"));
                if ( StringUtils.isBlank(repositoryKey) ) {
                    continue;
                }
                var authorId = StringUtils.defaultString(row.get("authorId"));
                if ( StringUtils.isBlank(authorId) ) {
                    var expressionInput = NcdReportContributorHelper.createExpressionInput(
                            StringUtils.defaultString(row.get("authorName")),
                            StringUtils.defaultString(row.get("authorEmail")));
                    authorId = NcdReportContributorHelper.computeAuthorId(expressionInput);
                }
                contributorIdsByRepositoryKey.computeIfAbsent(repositoryKey, k -> new HashSet<>()).add(authorId);
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

    private List<Map<String, String>> readCsvRows(NcdReportReader reader, String entryName) {
        try ( var csvReader = reader.bufferedReader(entryName) ) {
            var schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<Map<String, String>> iterator = CSV_MAPPER
                    .readerFor(new TypeReference<Map<String, String>>() {})
                    .with(schema)
                    .readValues(csvReader);
            var result = new ArrayList<Map<String, String>>();
            while ( iterator.hasNext() ) {
                result.add(iterator.next());
            }
            return result;
        } catch ( Exception e ) {
            throw new FcliSimpleException("Error reading %s from %s:\n\tMessage: %s", entryName, reader.getReportPath(), e.getMessage());
        }
    }

    private ObjectNode createOutputRow(String repositoryUrl, String repositoryName, String visibility, String fork, String status,
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
