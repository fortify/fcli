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

import java.io.BufferedWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.report.cli.cmd.AbstractReportGenerateCommand;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.license.ncd_report.config.NcdReportConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportContributorConfig;
import com.fortify.cli.license.ncd_report.descriptor.NcdReportSummaryDescriptor;
import com.fortify.cli.license.ncd_report.helper.NcdReportContributorHelper;
import com.fortify.cli.license.ncd_report.reader.NcdReportReader;
import com.fortify.cli.license.ncd_report.validator.NcdReportValidator;
import com.fortify.cli.license.ncd_report.writer.NcdReportContributorsCsvSchema;
import com.fortify.cli.license.ncd_report.writer.NcdReportRepositoriesWriter.NcdReportRepositoryReportingStatus;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "merge")
public final class NcdReportMergeCommand extends AbstractReportGenerateCommand {
    private static final ObjectMapper YAML_MAPPER = createYamlMapper();
    private static final CsvMapper CSV_MAPPER = new CsvMapper();
    private static final List<String> DETAIL_FILE_NAMES = List.of(
            "details/repositories.csv",
            "details/commits-by-branch.csv",
            "details/commits-by-repository.csv",
            "details/contributors-by-repository.csv");

    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;

    @Option(names = {"-r", "--reports"}, required = true, split = ",")
    @Getter private List<Path> reportPaths;
    private NcdReportSummaryDescriptor summary;

    @Override
    protected String getReportTitle() {
        return "Number of Contributing Developers (NCD) Merged Report";
    }

    @Override
    protected void generateReport(IReportWriter reportWriter) {
        this.summary = NcdReportSummaryDescriptor.fromObjectNode(reportWriter.summary());
        var sourceReports = loadSourceReports();
        addDateRangeToSummary(sourceReports);
        var mergedContributorConfig = mergeContributorConfig(sourceReports);
        var contributors = mergeContributors(sourceReports, mergedContributorConfig);
        synthesizeMissingDuplicateOf(contributors);
        writeMergedContributors(reportWriter, contributors);
        var mergedDetailStats = writeMergedDetails(reportWriter, sourceReports, contributors);
        writeMergedConfig(reportWriter, mergedContributorConfig);
        copyEmbeddedSources(reportWriter, sourceReports);
        updateSummary(contributors, sourceReports, mergedContributorConfig, mergedDetailStats);
        summary.applyTo(reportWriter.summary());
    }

    private void addDateRangeToSummary(List<SourceReport> sources) {
        var reportEndDate = sources.stream()
            .map(SourceReport::sourceReportEndDate)
            .filter(d -> d != null)
            .max(LocalDate::compareTo)
            .orElse(null);
        var reportStartDate = sources.stream()
            .map(SourceReport::sourceReportStartDate)
            .filter(d -> d != null)
            .min(LocalDate::compareTo)
            .orElse(null);

        if ( reportStartDate != null ) {
            summary.setReportStartDate(reportStartDate.toString());
        }
        if ( reportEndDate != null ) {
            summary.setReportEndDate(reportEndDate.toString());
        }
    }

    private List<SourceReport> loadSourceReports() {
        var result = new ArrayList<SourceReport>();
        var checksumErrors = new ArrayList<String>();
        var usedSourceNames = new LinkedHashSet<String>();
        for ( var reportPath : reportPaths ) {
            var sourceName = createUniqueSourceName(reportPath, usedSourceNames);
            usedSourceNames.add(sourceName);
            var sourceRef = "sources/" + sourceName;
            try ( var reader = new NcdReportReader(reportPath) ) {
                checksumErrors.addAll(NcdReportValidator.validateChecksums(reader));
                var config = reader.readConfig();
                var sourceSummary = reader.readSummary();
                var contributors = readContributors(reader, sourceRef);
                var entryNames = reader.listFileEntries();
                var contributorConfig = config.getContributor().orElseGet(NcdReportContributorConfig::new);
                result.add(new SourceReport(reportPath.toAbsolutePath(), sourceRef, contributors, entryNames, contributorConfig, config, sourceSummary));
            }
        }
        if ( !checksumErrors.isEmpty() ) {
            throw new FcliSimpleException("Checksum validation failed for source reports:\n\t%s", String.join("\n\t", checksumErrors));
        }
        return result;
    }

    private List<ContributorRecord> readContributors(NcdReportReader reader, String sourceRef) {
        try {
            var result = new ArrayList<ContributorRecord>();
            try ( var contributors = reader.readContributorsAsObjectNodeStream() ) {
                contributors.forEach(row -> {
                    var mapRow = JsonHelper.getObjectMapper().convertValue(row, new TypeReference<Map<String, String>>() {});
                    result.add(ContributorRecord.fromRow(mapRow, sourceRef));
                });
            }
            return result;
        } catch ( Exception e ) {
            throw new FcliSimpleException("Error reading contributors.csv from %s:\n\tMessage: %s", reader.getReportPath(), e.getMessage());
        }
    }

    private Optional<NcdReportContributorConfig> mergeContributorConfig(List<SourceReport> sourceReports) {
        var ignoreExpressions = collectDistinctExpressions(sourceReports, true);
        var duplicateExpressions = collectDistinctExpressions(sourceReports, false);

        if ( ignoreExpressions.isEmpty() && duplicateExpressions.isEmpty() ) {
            return Optional.empty();
        }

        var result = new NcdReportContributorConfig();
        result.setIgnoreExpression(combineExpressions(ignoreExpressions));
        result.setDuplicateExpression(combineExpressions(duplicateExpressions));
        return Optional.of(result);
    }

    private List<String> collectDistinctExpressions(List<SourceReport> sourceReports, boolean ignore) {
        var result = new ArrayList<String>();
        for ( var source : sourceReports ) {
            var expression = ignore
                    ? source.contributorConfig().getIgnoreExpression()
                    : source.contributorConfig().getDuplicateExpression();
            expression
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .filter(e -> !result.contains(e))
                .ifPresent(result::add);
        }
        return result;
    }

    private Optional<String> combineExpressions(List<String> expressions) {
        if ( expressions.isEmpty() ) {
            return Optional.empty();
        } else if ( expressions.size() == 1 ) {
            return Optional.of(expressions.get(0));
        }
        var combined = expressions.stream()
                .map(e -> "(" + e + ")")
                .collect(Collectors.joining(" ||\n"));
        return Optional.of(combined);
    }

    private List<ContributorRecord> mergeContributors(List<SourceReport> sourceReports, Optional<NcdReportContributorConfig> mergedContributorConfig) {
        List<ContributorRecord> allContributors = sourceReports.stream()
                .flatMap(s -> s.contributors().stream())
                .collect(Collectors.toCollection(ArrayList::new));
        allContributors = aggregateByAuthorId(allContributors);
        allContributors.forEach(c -> c.dormantStatus(c.sourceDormantStatus()));

        var parser = new SpelExpressionParser();
        var ignoreExpression = mergedContributorConfig
                .flatMap(NcdReportContributorConfig::getIgnoreExpression)
                .map(parser::parseExpression);
        var duplicateExpression = mergedContributorConfig
                .flatMap(NcdReportContributorConfig::getDuplicateExpression)
                .map(parser::parseExpression);

        int nextAuthorNumber = 1;
        var ignoredContributors = new ArrayList<ContributorRecord>();
        var nonIgnoredContributors = new ArrayList<ContributorRecord>();
        for ( var contributor : allContributors ) {
            // Preserve source rows that were already marked ignored in the original report.
            if ( contributor.isSourceIgnored() || isIgnored(contributor, ignoreExpression) ) {
                contributor.authorState("ignored");
                contributor.authorNumber(-1);
                contributor.contributionStatus("ignored");
                contributor.contributingAuthorNumber(-1);
                ignoredContributors.add(contributor);
            } else {
                contributor.authorState("processed");
                contributor.authorNumber(nextAuthorNumber++);
                nonIgnoredContributors.add(contributor);
            }
        }

        applyDeduplication(nonIgnoredContributors, duplicateExpression);

        var result = new ArrayList<ContributorRecord>(ignoredContributors.size() + nonIgnoredContributors.size());
        result.addAll(ignoredContributors);
        result.addAll(nonIgnoredContributors);
        return result;
    }

    private List<ContributorRecord> aggregateByAuthorId(List<ContributorRecord> contributors) {
        var byAuthorId = new TreeMap<String, ContributorRecord>();
        for ( var contributor : contributors ) {
            byAuthorId.compute(contributor.authorId(), (k, existing) -> {
                if ( existing == null ) {
                    return contributor;
                }
                existing.addSourceOccurrence(contributor.sourceReport(), contributor.sourceContributionStatus(),
                    contributor.sourceContributingAuthorNumber(), contributor.dormantStatus());
                return existing;
            });
        }
        return new ArrayList<>(byAuthorId.values());
    }

    private boolean isIgnored(ContributorRecord contributor, Optional<Expression> ignoreExpression) {
        return ignoreExpression
                .map(e -> JsonHelper.evaluateSpelExpression(contributor.expressionInput(), e, Boolean.class))
                .orElse(false);
    }

    private void applyDeduplication(List<ContributorRecord> contributors, Optional<Expression> duplicateExpression) {
        int count = contributors.size();
        if ( count == 0 ) {
            return;
        }

        var parent = new int[count];
        for ( int i = 0; i < count; i++ ) {
            parent[i] = i;
        }

        if ( duplicateExpression.isPresent() ) {
            var expr = duplicateExpression.get();
            for ( int i = 0; i < count; i++ ) {
                for ( int j = i + 1; j < count; j++ ) {
                    if ( isDuplicate(contributors.get(i), contributors.get(j), expr) ) {
                        union(parent, i, j);
                    }
                }
            }
        }

        var rootToMinIndex = new TreeMap<Integer, Integer>();
        for ( int i = 0; i < count; i++ ) {
            var root = find(parent, i);
            var currentIndex = i;
            rootToMinIndex.compute(root, (k, v) -> v == null ? currentIndex : Math.min(v, currentIndex));
        }

        var sortedRootEntries = rootToMinIndex.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toList());

        var rootToContributingNumber = new HashMap<Integer, Integer>();
        var rootToDormantStatus = new HashMap<Integer, DormantStatus>();
        int contributingAuthorNumber = 1;
        for ( var entry : sortedRootEntries ) {
            rootToContributingNumber.put(entry.getKey(), contributingAuthorNumber++);
            rootToDormantStatus.put(entry.getKey(), DormantStatus.DORMANT);
        }

        for ( int i = 0; i < count; i++ ) {
            var root = find(parent, i);
            var contributorDormantStatus = contributors.get(i).dormantStatus();
            rootToDormantStatus.merge(root, contributorDormantStatus, DormantStatus::merge);
        }

        for ( int i = 0; i < count; i++ ) {
            var root = find(parent, i);
            var representativeIndex = rootToMinIndex.get(root);
            var contributor = contributors.get(i);
            contributor.contributingAuthorNumber(rootToContributingNumber.get(root));
            contributor.dormantStatus(rootToDormantStatus.getOrDefault(root, DormantStatus.UNKNOWN));
            if ( i == representativeIndex ) {
                contributor.contributionStatus("contributing");
            } else {
                contributor.contributionStatus("duplicate");
                contributor.duplicateOf(contributors.get(representativeIndex).authorId());
            }
        }
    }

    private void synthesizeMissingDuplicateOf(List<ContributorRecord> contributors) {
        // For records marked as duplicate but with missing duplicateOf, synthesize it
        // by looking up the representative record (same contributingAuthorNumber with contributing status)
        var representativesByNumber = new HashMap<Integer, String>();
        
        for ( var contributor : contributors ) {
            if ( "contributing".equalsIgnoreCase(contributor.contributionStatus()) ) {
                representativesByNumber.put(contributor.contributingAuthorNumber(), contributor.authorId());
            }
        }
        
        for ( var contributor : contributors ) {
            if ( "duplicate".equalsIgnoreCase(contributor.contributionStatus()) 
                    && StringUtils.isBlank(contributor.duplicateOf) ) {
                var representativeId = representativesByNumber.get(contributor.contributingAuthorNumber());
                if ( representativeId != null ) {
                    contributor.duplicateOf(representativeId);
                }
            }
        }
    }

    private boolean isDuplicate(ContributorRecord c1, ContributorRecord c2, Expression expression) {
        return JsonHelper.evaluateSpelExpression(createCompareNode(c1, c2), expression, Boolean.class)
                || JsonHelper.evaluateSpelExpression(createCompareNode(c2, c1), expression, Boolean.class);
    }

    private ObjectNode createCompareNode(ContributorRecord c1, ContributorRecord c2) {
        var node = JsonHelper.getObjectMapper().createObjectNode();
        node.set("a1", c1.expressionInput());
        node.set("a2", c2.expressionInput());
        return node;
    }

    private void writeMergedContributors(IReportWriter reportWriter, List<ContributorRecord> contributors) {
        var writer = reportWriter.recordWriter(RecordWriterFactory.csv, "contributors.csv", false, null);
        // Convert to ObjectNode for sorting, then write sorted records
        var records = contributors.stream().map(ContributorRecord::toRow).collect(Collectors.toList());
        var sorted = NcdReportContributorsCsvSchema.sortByAuthorNameAndStatus(records);
        sorted.forEach(writer::append);
    }

    private MergedDetailStats writeMergedDetails(IReportWriter reportWriter, List<SourceReport> sourceReports, List<ContributorRecord> mergedContributors) {
        var repositoryRawCountsBySource = computeRepositoryRawCountsBySourceReport(sourceReports);
        var sourceSemanticsByKey = sourceReports.stream()
            .flatMap(s -> s.contributors().stream())
            .collect(Collectors.toMap(
                c -> sourceAndAuthorIdKey(c.sourceReport(), c.authorId()),
                c -> c,
                (a, b) -> a,
                HashMap::new));
        var mergedSemanticsBySourceAndAuthor = mergedContributors.stream()
            .collect(Collectors.toMap(
                c -> sourceAndAuthorIdKey(c.sourceReport(), c.authorId()),
                c -> c,
                (a, b) -> a,
                HashMap::new));
        var mergedSemanticsByAuthor = mergedContributors.stream()
            .collect(Collectors.toMap(
                ContributorRecord::authorId,
                c -> c,
                this::preferMoreSpecificMergedSemantics,
                HashMap::new));

        var countsByFile = new HashMap<String, Integer>();
        var repositoryCounts = new NcdReportSummaryDescriptor.RepositoryCounts();
        repositoryCounts.setIncluded(0);
        repositoryCounts.setExcluded(0);
        repositoryCounts.setEmpty(0);
        repositoryCounts.setError(0);
        repositoryCounts.setDormant(0);
        var repositoryWriter = reportWriter.recordWriter(RecordWriterFactory.csv, "repositories.csv", false, null);
        for ( var detailFileName : DETAIL_FILE_NAMES ) {
            var writer = reportWriter.recordWriter(RecordWriterFactory.csv, detailFileName, false, null);
            int fileRowCount = 0;
            for ( var sourceReport : sourceReports ) {
                try ( var reader = new NcdReportReader(sourceReport.originalPath()) ) {
                    if ( !sourceReport.entryNames().contains(detailFileName) ) {
                        continue;
                    }
                    for ( var row : readCsvRows(reader, detailFileName) ) {
                        normalizeDetailRow(detailFileName, row);
                        if ( "details/repositories.csv".equals(detailFileName) ) {
                            updateRepositoryCounts(repositoryCounts, row);
                        }
                        row.put("sourceReport", sourceReport.sourceRef());
                        enrichRowWithSemantics(row, sourceSemanticsByKey, mergedSemanticsBySourceAndAuthor, mergedSemanticsByAuthor);
                        if ( "details/repositories.csv".equals(detailFileName) ) {
                            repositoryWriter.append(createTopLevelRepositoryRow(row,
                                    repositoryRawCountsBySource.getOrDefault(sourceReport.sourceRef(), Collections.emptyMap())));
                        }
                        writer.append(JsonHelper.getObjectMapper().valueToTree(row));
                        fileRowCount++;
                    }
                }
            }
            countsByFile.put(detailFileName, fileRowCount);
        }
        repositoryCounts.setTotal(countsByFile.getOrDefault("details/repositories.csv", 0));
        return new MergedDetailStats(countsByFile, repositoryCounts);
    }

    private Map<String, Map<String, RepositoryRawCounts>> computeRepositoryRawCountsBySourceReport(List<SourceReport> sourceReports) {
        var result = new HashMap<String, Map<String, RepositoryRawCounts>>();
        for ( var sourceReport : sourceReports ) {
            var commitCountsByRepository = new HashMap<String, Integer>();
            var contributorIdsByRepository = new HashMap<String, Set<String>>();
            try ( var reader = new NcdReportReader(sourceReport.originalPath()) ) {
                if ( sourceReport.entryNames().contains("details/commits-by-repository.csv") ) {
                    for ( var row : readCsvRows(reader, "details/commits-by-repository.csv") ) {
                        var repositoryUrl = StringUtils.defaultString(row.get("repositoryUrl"));
                        if ( StringUtils.isBlank(repositoryUrl) ) {
                            continue;
                        }
                        commitCountsByRepository.put(repositoryUrl, commitCountsByRepository.getOrDefault(repositoryUrl, 0) + 1);
                    }
                }
                if ( sourceReport.entryNames().contains("details/contributors-by-repository.csv") ) {
                    for ( var row : readCsvRows(reader, "details/contributors-by-repository.csv") ) {
                        var repositoryUrl = StringUtils.defaultString(row.get("repositoryUrl"));
                        if ( StringUtils.isBlank(repositoryUrl) ) {
                            continue;
                        }
                        var authorId = StringUtils.defaultString(row.get("authorId"));
                        if ( StringUtils.isBlank(authorId) ) {
                            var expressionInput = NcdReportContributorHelper.createExpressionInput(
                                    StringUtils.defaultString(row.get("authorName")),
                                    StringUtils.defaultString(row.get("authorEmail")));
                            authorId = NcdReportContributorHelper.computeAuthorId(expressionInput);
                        }
                        contributorIdsByRepository.computeIfAbsent(repositoryUrl, k -> new HashSet<>()).add(authorId);
                    }
                }
            }

            var repositoryRawCounts = new HashMap<String, RepositoryRawCounts>();
            var repositoryUrls = new HashSet<String>();
            repositoryUrls.addAll(commitCountsByRepository.keySet());
            repositoryUrls.addAll(contributorIdsByRepository.keySet());
            repositoryUrls.forEach(repositoryUrl -> repositoryRawCounts.put(repositoryUrl,
                    new RepositoryRawCounts(
                            commitCountsByRepository.getOrDefault(repositoryUrl, 0),
                            contributorIdsByRepository.getOrDefault(repositoryUrl, Collections.emptySet()).size())));
            result.put(sourceReport.sourceRef(), repositoryRawCounts);
        }
        return result;
    }

    private ObjectNode createTopLevelRepositoryRow(Map<String, String> detailRow, Map<String, RepositoryRawCounts> rawCountsByRepositoryUrl) {
        var repositoryUrl = StringUtils.defaultString(detailRow.get("repositoryUrl"));
        var rawCounts = rawCountsByRepositoryUrl.get(repositoryUrl);
        var result = JsonHelper.getObjectMapper().createObjectNode()
                .put("repositoryUrl", repositoryUrl)
                .put("repositoryName", StringUtils.defaultString(detailRow.get("repositoryName")))
                .put("visibility", StringUtils.defaultString(detailRow.get("visibility")))
                .put("fork", StringUtils.defaultString(detailRow.get("fork")))
                .put("status", StringUtils.defaultString(detailRow.get("status")))
                .put("reason", StringUtils.defaultString(detailRow.get("reason")))
                .put("dormant", StringUtils.defaultString(detailRow.get("dormant")));
        if ( rawCounts == null ) {
            result.putNull("commitCountRaw");
            result.putNull("contributorCountRaw");
        } else {
            result.put("commitCountRaw", rawCounts.commitCountRaw());
            result.put("contributorCountRaw", rawCounts.contributorCountRaw());
        }
        result.put("sourceReport", StringUtils.defaultString(detailRow.get("sourceReport")));
        return result;
    }

    private void updateRepositoryCounts(NcdReportSummaryDescriptor.RepositoryCounts repositoryCounts, Map<String, String> row) {
        var status = StringUtils.defaultString(row.get("status"));
        if ( NcdReportRepositoryReportingStatus.included.name().equalsIgnoreCase(status) ) {
            repositoryCounts.setIncluded(increaseCount(repositoryCounts.getIncluded()));
            var dormant = StringUtils.defaultString(row.get("dormant"));
            if ( "true".equalsIgnoreCase(dormant) ) {
                repositoryCounts.setDormant(increaseCount(repositoryCounts.getDormant()));
            }
        } else if ( NcdReportRepositoryReportingStatus.excluded.name().equalsIgnoreCase(status) ) {
            repositoryCounts.setExcluded(increaseCount(repositoryCounts.getExcluded()));
        } else if ( NcdReportRepositoryReportingStatus.empty.name().equalsIgnoreCase(status) ) {
            repositoryCounts.setEmpty(increaseCount(repositoryCounts.getEmpty()));
        } else if ( NcdReportRepositoryReportingStatus.error.name().equalsIgnoreCase(status) ) {
            repositoryCounts.setError(increaseCount(repositoryCounts.getError()));
        }
    }

    private int increaseCount(Integer count) {
        return (count == null ? 0 : count) + 1;
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

    private void normalizeDetailRow(String detailFileName, Map<String, String> row) {
        if ( !row.containsKey("dormant") || StringUtils.isBlank(row.get("dormant")) ) {
            row.put("dormant", "unknown");
        }

        if ( isAuthorDetailFile(detailFileName) ) {
            var authorId = StringUtils.defaultString(row.get("authorId"));
            if ( StringUtils.isBlank(authorId) ) {
                var expressionInput = NcdReportContributorHelper.createExpressionInput(
                        StringUtils.defaultString(row.get("authorName")),
                        StringUtils.defaultString(row.get("authorEmail")));
                row.put("authorId", NcdReportContributorHelper.computeAuthorId(expressionInput));
            }
        }
    }

    private boolean isAuthorDetailFile(String detailFileName) {
        return "details/commits-by-branch.csv".equals(detailFileName)
                || "details/commits-by-repository.csv".equals(detailFileName)
                || "details/contributors-by-repository.csv".equals(detailFileName);
    }

    private void enrichRowWithSemantics(
            Map<String, String> row,
            Map<String, ContributorRecord> sourceSemanticsByKey,
            Map<String, ContributorRecord> mergedSemanticsBySourceAndAuthor,
            Map<String, ContributorRecord> mergedSemanticsByAuthor)
    {
        var sourceReport = StringUtils.defaultString(row.get("sourceReport"));
        var authorId = StringUtils.defaultString(row.get("authorId"));
        if ( StringUtils.isBlank(authorId) ) { return; }

        row.put("sourceAuthorId", authorId);
        row.put("sourceAuthorState", StringUtils.defaultString(row.get("authorState")));

        var sourceSemantics = sourceSemanticsByKey.get(sourceAndAuthorIdKey(sourceReport, authorId));
        if ( sourceSemantics != null ) {
            row.put("sourceContributionStatus", sourceSemantics.sourceContributionStatus());
            row.put("sourceDormant", sourceSemantics.sourceDormantStatus().toOutputValue());
        }

        var mergedSemantics = mergedSemanticsBySourceAndAuthor.get(sourceAndAuthorIdKey(sourceReport, authorId));
        if ( mergedSemantics == null ) {
            mergedSemantics = mergedSemanticsByAuthor.get(authorId);
        }
        if ( mergedSemantics != null ) {
            row.put("mergedAuthorId", mergedSemantics.authorId());
            row.put("mergedAuthorState", mergedSemantics.authorState());
            row.put("mergedContributionStatus", mergedSemantics.contributionStatus());
            row.put("mergedDormant", mergedSemantics.dormantStatus().toOutputValue());
            row.put("dormant", mergedSemantics.dormantStatus().toOutputValue());
        }
    }

    private String sourceAndAuthorIdKey(String sourceReport, String authorId) {
        return sourceReport + "|" + authorId;
    }

    private ContributorRecord preferMoreSpecificMergedSemantics(ContributorRecord c1, ContributorRecord c2) {
        var priority1 = mergedStatusPriority(c1.contributionStatus());
        var priority2 = mergedStatusPriority(c2.contributionStatus());
        if ( priority1 != priority2 ) {
            return priority1 > priority2 ? c1 : c2;
        }
        return c1.authorNumber() <= c2.authorNumber() ? c1 : c2;
    }

    private int mergedStatusPriority(String status) {
        if ( "contributing".equals(status) ) { return 3; }
        if ( "duplicate".equals(status) ) { return 2; }
        if ( "ignored".equals(status) ) { return 1; }
        return 0;
    }

    private void writeMergedConfig(IReportWriter reportWriter, Optional<NcdReportContributorConfig> contributorConfig) {
        var mergedConfig = new NcdReportConfig();
        mergedConfig.setContributor(contributorConfig);
        try ( BufferedWriter bw = reportWriter.bufferedWriter("report-config.yaml") ) {
            bw.write("# Auto-generated merged NCD report configuration\n");
            bw.write(YAML_MAPPER.writeValueAsString(mergedConfig));
        } catch ( Exception e ) {
            throw new FcliSimpleException("Error writing merged report-config.yaml:\n\tMessage: %s", e.getMessage());
        }
    }

    private void copyEmbeddedSources(IReportWriter reportWriter, List<SourceReport> sourceReports) {
        for ( var sourceReport : sourceReports ) {
            try ( var reader = new NcdReportReader(sourceReport.originalPath()) ) {
                for ( var entryName : sourceReport.entryNames() ) {
                    var targetEntryName = sourceReport.sourceRef() + "/" + entryName;
                    reportWriter.copyTextFile(reader.entryPath(entryName), targetEntryName);
                }
            }
        }
    }

        private void updateSummary(List<ContributorRecord> contributors, List<SourceReport> sources,
            Optional<NcdReportContributorConfig> mergedContributorConfig, MergedDetailStats mergedDetailStats)
    {
        summary.setMergedReportCount(sources.size());
        summary.setMergedSourceReports(sources.stream().map(SourceReport::sourceRef).toList());
        summary.setRepositoryCounts(mergedDetailStats.repositoryCounts());

        int total = contributors.size();
        int ignored = (int) contributors.stream().filter(c -> "ignored".equals(c.contributionStatus())).count();
        int duplicate = (int) contributors.stream().filter(c -> "duplicate".equals(c.contributionStatus())).count();
        int contributing = (int) contributors.stream().filter(c -> "contributing".equals(c.contributionStatus())).count();
        int dormant = (int) contributors.stream()
            .filter(c -> "contributing".equals(c.contributionStatus()))
            .filter(c -> c.dormantStatus() == DormantStatus.DORMANT)
            .count();
        int nonIgnored = total - ignored;

        var authorCount = new NcdReportSummaryDescriptor.AuthorCount();
        authorCount.setTotal(total);
        authorCount.setContributing(contributing);
        authorCount.setIgnored(ignored);
        authorCount.setNonIgnored(nonIgnored);
        authorCount.setDuplicate(duplicate);
        authorCount.setDormant(dormant);
        summary.setAuthorCount(authorCount);

        var commitCount = new NcdReportSummaryDescriptor.CommitCount();
        commitCount.setAnalyzed(mergedDetailStats.detailRowCounts().getOrDefault("details/commits-by-branch.csv", 0));
        summary.setCommitCount(commitCount);

        var detailRowCount = new NcdReportSummaryDescriptor.DetailRowCount();
        detailRowCount.setRepositories(mergedDetailStats.detailRowCounts().getOrDefault("details/repositories.csv", 0));
        detailRowCount.setCommitsByBranch(mergedDetailStats.detailRowCounts().getOrDefault("details/commits-by-branch.csv", 0));
        detailRowCount.setCommitsByRepository(mergedDetailStats.detailRowCounts().getOrDefault("details/commits-by-repository.csv", 0));
        detailRowCount.setContributorsByRepository(mergedDetailStats.detailRowCounts().getOrDefault("details/contributors-by-repository.csv", 0));
        summary.setDetailRowCount(detailRowCount);

        mergedContributorConfig
            .flatMap(NcdReportContributorConfig::getDuplicateExpression)
            .ifPresent(summary::setMergedDuplicateExpression);
    }

            private static record MergedDetailStats(
                Map<String, Integer> detailRowCounts,
                NcdReportSummaryDescriptor.RepositoryCounts repositoryCounts
            ) {}

    private static record RepositoryRawCounts(
            int commitCountRaw,
            int contributorCountRaw
    ) {}

    private String createUniqueSourceName(Path reportPath, Set<String> usedNames) {
        String fileName = reportPath.getFileName().toString();
        String baseName = fileName.replaceFirst("(?i)\\.zip$", "");
        if ( StringUtils.isBlank(baseName) ) {
            baseName = "report";
        }
        String sanitized = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String candidate = sanitized;
        int index = 2;
        while ( usedNames.contains(candidate) ) {
            candidate = sanitized + "-" + index++;
        }
        return candidate;
    }

    private static int find(int[] parent, int i) {
        if ( parent[i] != i ) {
            parent[i] = find(parent, parent[i]);
        }
        return parent[i];
    }

    private static void union(int[] parent, int i, int j) {
        int ri = find(parent, i);
        int rj = find(parent, j);
        if ( ri != rj ) {
            parent[rj] = ri;
        }
    }

    private static ObjectMapper createYamlMapper() {
        var mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    private static final class ContributorRecord {
        private final String authorName;
        private final String authorEmail;
        private final String sourceReport;
        private final Set<String> sourceReports;
        private final Set<String> sourceContributionStatuses;
        private final Set<DormantStatus> sourceDormantValues;
        private final int sourceContributingAuthorNumber;
        private final ObjectNode expressionInput;

        private String authorId;
        private String authorState;
        private int authorNumber;
        private String contributionStatus;
        private int contributingAuthorNumber;
        private String duplicateOf;
        private String overrideStatus;
        private String overrideStatusConfidence;
        private String overrideStatusNotes;
        private DormantStatus dormantStatus;

        private ContributorRecord(String authorName, String authorEmail, String sourceReport, String sourceContributionStatus,
            int sourceContributingAuthorNumber, DormantStatus sourceDormantStatus, ObjectNode expressionInput)
        {
            this.authorName = authorName;
            this.authorEmail = authorEmail;
            this.sourceReport = sourceReport;
            this.sourceReports = new LinkedHashSet<>();
            this.sourceReports.add(sourceReport);
            this.sourceContributionStatuses = new LinkedHashSet<>();
            this.sourceContributionStatuses.add(sourceContributionStatus);
            this.sourceDormantValues = new LinkedHashSet<>();
            this.sourceDormantValues.add(sourceDormantStatus);
            this.sourceContributingAuthorNumber = sourceContributingAuthorNumber;
            this.expressionInput = expressionInput;
            this.authorId = NcdReportContributorHelper.computeAuthorId(expressionInput);
            this.authorState = "processed";
            this.authorNumber = -1;
            this.contributionStatus = "contributing";
            this.contributingAuthorNumber = -1;
            this.duplicateOf = "";
            this.overrideStatus = "";
            this.overrideStatusConfidence = "";
            this.overrideStatusNotes = "";
            this.dormantStatus = sourceDormantStatus;
        }

        static ContributorRecord fromRow(Map<String, String> row, String sourceReport) {
            String authorName = StringUtils.defaultString(row.get("authorName"));
            String authorEmail = StringUtils.defaultString(row.get("authorEmail"));
            String sourceContributionStatus = StringUtils.defaultString(row.get("contributionStatus"));
            DormantStatus sourceDormantStatus = DormantStatus.fromInput(row.get("dormant"));
            int sourceContributingAuthorNumber = parseInt(row.get("contributingAuthorNumber"), -1);
            ObjectNode expressionInput = NcdReportContributorHelper.createExpressionInput(authorName, authorEmail);
                            var record = new ContributorRecord(authorName, authorEmail, sourceReport, sourceContributionStatus,
                                            sourceContributingAuthorNumber, sourceDormantStatus, expressionInput);
              // Preserve any existing override fields from source report
              record.duplicateOf = StringUtils.defaultString(row.get("duplicateOf"));
              record.overrideStatus = StringUtils.defaultString(row.get("overrideStatus"));
              record.overrideStatusConfidence = StringUtils.defaultString(row.get("overrideStatusConfidence"));
              record.overrideStatusNotes = StringUtils.defaultString(row.get("overrideStatusNotes"));
              return record;
        }

        ObjectNode toRow() {
            return JsonHelper.getObjectMapper().createObjectNode()
                    .put("authorId", authorId)
                    .put("authorName", authorName)
                    .put("authorEmail", authorEmail)
                    .put("authorState", authorState)
                    .put("contributionStatus", contributionStatus)
                    .put("dormant", dormantStatus.toOutputValue())
                    .put("duplicateOf", duplicateOf)
                    .put("overrideStatus", overrideStatus)
                    .put("overrideStatusConfidence", overrideStatusConfidence)
                    .put("overrideStatusNotes", overrideStatusNotes)
                    .put("sourceReports", String.join(";", sourceReports))
                    .put("sourceContributionStatus", sourceContributionStatus());
        }

        ObjectNode expressionInput() {
            return expressionInput;
        }

        String authorId() {
            return authorId;
        }

        String sourceReport() {
            return sourceReport;
        }

        String sourceContributionStatus() {
            return sourceContributionStatuses.size() == 1
                    ? sourceContributionStatuses.iterator().next()
                    : String.join(";", sourceContributionStatuses);
        }

        int sourceContributingAuthorNumber() {
            return sourceContributingAuthorNumber;
        }

        DormantStatus sourceDormantStatus() {
            if ( sourceDormantValues.contains(DormantStatus.ACTIVE) ) {
                return DormantStatus.ACTIVE;
            }
            if ( sourceDormantValues.contains(DormantStatus.UNKNOWN) ) {
                return DormantStatus.UNKNOWN;
            }
            return DormantStatus.DORMANT;
        }

        String authorState() {
            return authorState;
        }

        int authorNumber() {
            return authorNumber;
        }

        boolean isSourceContributing() {
            return sourceContributionStatuses.stream().anyMatch(s -> "contributing".equalsIgnoreCase(s));
        }

        boolean isSourceIgnored() {
            return sourceContributionStatuses.stream().anyMatch(s -> "ignored".equalsIgnoreCase(s));
        }

        void addSourceOccurrence(String sourceReport, String sourceContributionStatus, int sourceContributingAuthorNumber,
                DormantStatus sourceDormantStatus)
        {
            this.sourceReports.add(sourceReport);
            this.sourceContributionStatuses.add(sourceContributionStatus);
            this.sourceDormantValues.add(sourceDormantStatus);
        }

        void authorState(String authorState) {
            this.authorState = authorState;
        }

        void authorNumber(int authorNumber) {
            this.authorNumber = authorNumber;
        }

        void contributionStatus(String contributionStatus) {
            this.contributionStatus = contributionStatus;
        }

        String contributionStatus() {
            return contributionStatus;
        }

        int contributingAuthorNumber() {
            return contributingAuthorNumber;
        }

        void contributingAuthorNumber(int contributingAuthorNumber) {
            this.contributingAuthorNumber = contributingAuthorNumber;
        }

        void duplicateOf(String duplicateOf) {
            this.duplicateOf = duplicateOf;
        }

        DormantStatus dormantStatus() {
            return dormantStatus;
        }

        void dormantStatus(DormantStatus dormantStatus) {
            this.dormantStatus = dormantStatus;
        }

        private static int parseInt(String value, int defaultValue) {
            try {
                return Integer.parseInt(StringUtils.defaultIfBlank(value, String.valueOf(defaultValue)));
            } catch ( NumberFormatException e ) {
                return defaultValue;
            }
        }

    }

    private static enum DormantStatus {
        DORMANT,
        ACTIVE,
        UNKNOWN;

        private static DormantStatus merge(DormantStatus a, DormantStatus b) {
            if ( a == ACTIVE || b == ACTIVE ) {
                return ACTIVE;
            }
            if ( a == UNKNOWN || b == UNKNOWN ) {
                return UNKNOWN;
            }
            return DORMANT;
        }

        private static DormantStatus fromInput(String value) {
            var normalized = StringUtils.defaultString(value).trim().toLowerCase();
            return switch ( normalized ) {
            case "true" -> DORMANT;
            case "false" -> ACTIVE;
            default -> UNKNOWN;
            };
        }

        private String toOutputValue() {
            return switch ( this ) {
            case DORMANT -> "true";
            case ACTIVE -> "false";
            case UNKNOWN -> "unknown";
            };
        }
    }

    private static record SourceReport(
            Path originalPath,
            String sourceRef,
            List<ContributorRecord> contributors,
            List<String> entryNames,
            NcdReportContributorConfig contributorConfig,
            NcdReportConfig config,
            ObjectNode sourceSummary
    ) {
        SourceReport {
            contributors = Collections.unmodifiableList(contributors);
            entryNames = Collections.unmodifiableList(entryNames);
        }

        LocalDate sourceReportStartDate() {
            return parseSummaryDate("reportStartDate");
        }

        LocalDate sourceReportEndDate() {
            return parseSummaryDate("reportEndDate");
        }

        private LocalDate parseSummaryDate(String propertyName) {
            var value = sourceSummary.path(propertyName).asText("");
            if ( StringUtils.isBlank(value) ) {
                return null;
            }
            try {
                return LocalDate.parse(value);
            } catch ( Exception e ) {
                throw new FcliTechnicalException(String.format(
                    "Invalid date '%s' for '%s' in source report %s",
                    value, propertyName, originalPath), e);
            }
        }
    }
}
