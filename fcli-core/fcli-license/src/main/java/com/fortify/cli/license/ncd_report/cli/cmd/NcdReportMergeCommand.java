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
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import java.util.regex.Pattern;
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
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.report.cli.cmd.AbstractReportGenerateCommand;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.license.ncd_report.config.NcdReportConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportContributorConfig;
import com.fortify.cli.license.ncd_report.reader.NcdReportReader;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "merge")
public final class NcdReportMergeCommand extends AbstractReportGenerateCommand {
    private static final ObjectMapper YAML_MAPPER = createYamlMapper();
    private static final CsvMapper CSV_MAPPER = new CsvMapper();
    private static final Pattern CHECKSUM_LINE_PATTERN = Pattern.compile("^([0-9a-fA-F]{64})\\s+\\*?(.+)$");
    private static final List<String> DETAIL_FILE_NAMES = List.of(
            "details/repositories.csv",
            "details/commits-by-branch.csv",
            "details/commits-by-repository.csv",
            "details/contributors-by-repository.csv");

    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;

    @Option(names = {"-r", "--reports"}, required = true, split = ",", arity = "2..*")
    @DisableTest(TestType.OPT_ARITY_VARIABLE)
    @Getter private List<Path> reportPaths;

    @Override
    protected String getReportTitle() {
        return "Number of Contributing Developers (NCD) Merged Report";
    }

    @Override
    protected void generateReport(IReportWriter reportWriter) {
        var sourceReports = loadSourceReports();
        var mergedContributorConfig = mergeContributorConfig(sourceReports);
        var contributors = mergeContributors(sourceReports, mergedContributorConfig);
        writeMergedContributors(reportWriter, contributors);
        var detailRowCounts = writeMergedDetails(reportWriter, sourceReports, contributors);
        writeMergedConfig(reportWriter, mergedContributorConfig);
        copyEmbeddedSources(reportWriter, sourceReports);
        updateSummary(reportWriter, contributors, sourceReports, mergedContributorConfig, detailRowCounts);
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
                checksumErrors.addAll(validateChecksums(reader));
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

    private List<String> validateChecksums(NcdReportReader reader) {
        var result = new ArrayList<String>();
        var checksumsPath = reader.entryPath("checksums.sha256");
        if ( !java.nio.file.Files.exists(checksumsPath) ) {
            result.add(String.format("%s: missing checksums.sha256", reader.getReportPath()));
            return result;
        }

        var expectedByEntry = new HashMap<String, String>();
        try ( var checksumsReader = reader.bufferedReader("checksums.sha256") ) {
            String line;
            int lineNumber = 0;
            while ( (line = checksumsReader.readLine()) != null ) {
                lineNumber++;
                if ( StringUtils.isBlank(line) ) {
                    continue;
                }
                var matcher = CHECKSUM_LINE_PATTERN.matcher(line.trim());
                if ( !matcher.matches() ) {
                    result.add(String.format("%s: checksums.sha256:%d invalid checksum format", reader.getReportPath(), lineNumber));
                    continue;
                }
                var checksum = matcher.group(1).toUpperCase();
                var entryName = matcher.group(2).trim();
                if ( StringUtils.isBlank(entryName) ) {
                    result.add(String.format("%s: checksums.sha256:%d missing entry name", reader.getReportPath(), lineNumber));
                    continue;
                }
                expectedByEntry.put(entryName, checksum);
            }
        } catch ( Exception e ) {
            result.add(String.format("%s: error reading checksums.sha256 (%s)", reader.getReportPath(), e.getMessage()));
            return result;
        }

        var availableEntries = new HashSet<>(reader.listFileEntries());
        availableEntries.remove("checksums.sha256");
        for ( var entryName : availableEntries ) {
            if ( !expectedByEntry.containsKey(entryName) ) {
                result.add(String.format("%s: missing checksum entry for file %s", reader.getReportPath(), entryName));
            }
        }

        expectedByEntry.forEach((entryName, expectedChecksum) -> {
            var entryPath = reader.entryPath(entryName);
            if ( !java.nio.file.Files.exists(entryPath) ) {
                result.add(String.format("%s: checksum references missing file %s", reader.getReportPath(), entryName));
                return;
            }
            try {
                var actualChecksum = sha256(entryPath);
                if ( !expectedChecksum.equalsIgnoreCase(actualChecksum) ) {
                    result.add(String.format(
                        "%s: checksum mismatch for %s (expected %s, actual %s)",
                        reader.getReportPath(), entryName, expectedChecksum, actualChecksum));
                }
            } catch ( Exception e ) {
                result.add(String.format("%s: error calculating checksum for %s (%s)", reader.getReportPath(), entryName, e.getMessage()));
            }
        });
        return result;
    }

    private List<ContributorRecord> readContributors(NcdReportReader reader, String sourceRef) {
        try ( var csvReader = reader.bufferedReader("contributors.csv") ) {
            var schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<Map<String, String>> iterator = CSV_MAPPER
                    .readerFor(new TypeReference<Map<String, String>>() {})
                    .with(schema)
                    .readValues(csvReader);
            var result = new ArrayList<ContributorRecord>();
            while ( iterator.hasNext() ) {
                var row = iterator.next();
                result.add(ContributorRecord.fromRow(row, sourceRef));
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
        var allContributors = sourceReports.stream()
                .flatMap(s -> s.contributors().stream())
                .filter(ContributorRecord::isSourceContributing)
                .collect(Collectors.toCollection(ArrayList::new));

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
            if ( isIgnored(contributor, ignoreExpression) ) {
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
        int contributingAuthorNumber = 1;
        for ( var entry : sortedRootEntries ) {
            rootToContributingNumber.put(entry.getKey(), contributingAuthorNumber++);
        }

        for ( int i = 0; i < count; i++ ) {
            var root = find(parent, i);
            var representativeIndex = rootToMinIndex.get(root);
            var contributor = contributors.get(i);
            contributor.contributingAuthorNumber(rootToContributingNumber.get(root));
            contributor.contributionStatus(i == representativeIndex ? "contributing" : "duplicate");
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
        contributors.forEach(c -> writer.append(c.toRow()));
    }

    private Map<String, Integer> writeMergedDetails(IReportWriter reportWriter, List<SourceReport> sourceReports, List<ContributorRecord> mergedContributors) {
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
        for ( var detailFileName : DETAIL_FILE_NAMES ) {
            var writer = reportWriter.recordWriter(RecordWriterFactory.csv, detailFileName, false, null);
            int fileRowCount = 0;
            for ( var sourceReport : sourceReports ) {
                try ( var reader = new NcdReportReader(sourceReport.originalPath()) ) {
                    if ( !sourceReport.entryNames().contains(detailFileName) ) {
                        continue;
                    }
                    for ( var row : readCsvRows(reader, detailFileName) ) {
                        row.put("sourceReport", sourceReport.sourceRef());
                        enrichRowWithSemantics(row, sourceSemanticsByKey, mergedSemanticsBySourceAndAuthor, mergedSemanticsByAuthor);
                        writer.append(JsonHelper.getObjectMapper().valueToTree(row));
                        fileRowCount++;
                    }
                }
            }
            countsByFile.put(detailFileName, fileRowCount);
        }
        return countsByFile;
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
        row.put("sourceAuthorNumber", StringUtils.defaultString(row.get("authorNumber")));

        var sourceSemantics = sourceSemanticsByKey.get(sourceAndAuthorIdKey(sourceReport, authorId));
        if ( sourceSemantics != null ) {
            row.put("sourceContributionStatus", sourceSemantics.sourceContributionStatus());
            row.put("sourceContributingAuthorNumber", String.valueOf(sourceSemantics.sourceContributingAuthorNumber()));
        }

        var mergedSemantics = mergedSemanticsBySourceAndAuthor.get(sourceAndAuthorIdKey(sourceReport, authorId));
        if ( mergedSemantics == null ) {
            mergedSemantics = mergedSemanticsByAuthor.get(authorId);
        }
        if ( mergedSemantics != null ) {
            row.put("mergedAuthorId", mergedSemantics.authorId());
            row.put("mergedAuthorState", mergedSemantics.authorState());
            row.put("mergedAuthorNumber", String.valueOf(mergedSemantics.authorNumber()));
            row.put("mergedContributionStatus", mergedSemantics.contributionStatus());
            row.put("mergedContributingAuthorNumber", String.valueOf(mergedSemantics.contributingAuthorNumber()));
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

        private void updateSummary(IReportWriter reportWriter, List<ContributorRecord> contributors, List<SourceReport> sources,
            Optional<NcdReportContributorConfig> mergedContributorConfig, Map<String, Integer> detailRowCounts)
    {
        var summary = reportWriter.summary();
        summary.put("mergedReportCount", sources.size());
        summary.set("mergedSourceReports", JsonHelper.toArrayNode(sources.stream().map(SourceReport::sourceRef).toArray(String[]::new)));

        int total = contributors.size();
        int ignored = (int) contributors.stream().filter(c -> "ignored".equals(c.contributionStatus())).count();
        int duplicate = (int) contributors.stream().filter(c -> "duplicate".equals(c.contributionStatus())).count();
        int contributing = (int) contributors.stream().filter(c -> "contributing".equals(c.contributionStatus())).count();
        int nonIgnored = total - ignored;

        summary.set("authorCount", JsonHelper.getObjectMapper().createObjectNode()
                .put("total", total)
                .put("contributing", contributing)
                .put("ignored", ignored)
                .put("nonIgnored", nonIgnored)
                .put("duplicate", duplicate));
        summary.set("commitCount", JsonHelper.getObjectMapper().createObjectNode()
            .put("analyzed", detailRowCounts.getOrDefault("details/commits-by-branch.csv", 0)));
        summary.set("detailRowCount", JsonHelper.getObjectMapper().createObjectNode()
            .put("repositories", detailRowCounts.getOrDefault("details/repositories.csv", 0))
            .put("commitsByBranch", detailRowCounts.getOrDefault("details/commits-by-branch.csv", 0))
            .put("commitsByRepository", detailRowCounts.getOrDefault("details/commits-by-repository.csv", 0))
            .put("contributorsByRepository", detailRowCounts.getOrDefault("details/contributors-by-repository.csv", 0)));

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
            summary.put("reportStartDate", reportStartDate.toString());
        }
        if ( reportEndDate != null ) {
            summary.put("reportEndDate", reportEndDate.toString());
        }

        mergedContributorConfig
            .flatMap(NcdReportContributorConfig::getDuplicateExpression)
            .ifPresent(e -> summary.put("mergedDuplicateExpression", e));
    }

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

    private static String computeAuthorId(ObjectNode expressionInput) {
        var cleanName = expressionInput.path("cleanName").asText("");
        var cleanEmailName = expressionInput.path("cleanEmailName").asText("");
        var input = cleanName + ":" + cleanEmailName;
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return String.format("%032x", new BigInteger(1, hash)).substring(0, 16);
        } catch ( NoSuchAlgorithmException e ) {
            throw new FcliBugException("SHA-256 not available", e);
        }
    }

    private static String sha256(Path path) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(path));
            return String.format("%064X", new BigInteger(1, hash));
        } catch ( NoSuchAlgorithmException e ) {
            throw new FcliBugException("SHA-256 not available", e);
        } catch ( Exception e ) {
            throw new FcliTechnicalException(String.format("Error calculating checksum for %s", path), e);
        }
    }

    private static final class ContributorRecord {
        private final String authorName;
        private final String authorEmail;
        private final String sourceReport;
        private final String sourceContributionStatus;
        private final int sourceContributingAuthorNumber;
        private final ObjectNode expressionInput;

        private String authorId;
        private String authorState;
        private int authorNumber;
        private String contributionStatus;
        private int contributingAuthorNumber;

        private ContributorRecord(String authorName, String authorEmail, String sourceReport, String sourceContributionStatus,
                int sourceContributingAuthorNumber, ObjectNode expressionInput)
        {
            this.authorName = authorName;
            this.authorEmail = authorEmail;
            this.sourceReport = sourceReport;
            this.sourceContributionStatus = sourceContributionStatus;
            this.sourceContributingAuthorNumber = sourceContributingAuthorNumber;
            this.expressionInput = expressionInput;
            this.authorId = computeAuthorId(expressionInput);
            this.authorState = "processed";
            this.authorNumber = -1;
            this.contributionStatus = "contributing";
            this.contributingAuthorNumber = -1;
        }

        static ContributorRecord fromRow(Map<String, String> row, String sourceReport) {
            String authorName = StringUtils.defaultString(row.get("authorName"));
            String authorEmail = StringUtils.defaultString(row.get("authorEmail"));
            String sourceContributionStatus = StringUtils.defaultString(row.get("contributionStatus"));
            int sourceContributingAuthorNumber = parseInt(row.get("contributingAuthorNumber"), -1);
            ObjectNode expressionInput = createExpressionInput(authorName, authorEmail);
            return new ContributorRecord(authorName, authorEmail, sourceReport, sourceContributionStatus, sourceContributingAuthorNumber, expressionInput);
        }

        ObjectNode toRow() {
            return JsonHelper.getObjectMapper().createObjectNode()
                    .put("authorId", authorId)
                    .put("authorName", authorName)
                    .put("authorEmail", authorEmail)
                    .put("authorState", authorState)
                    .put("authorNumber", authorNumber)
                    .put("contributionStatus", contributionStatus)
                    .put("contributingAuthorNumber", contributingAuthorNumber)
                    .put("sourceReport", sourceReport)
                    .put("sourceContributionStatus", sourceContributionStatus)
                    .put("sourceContributingAuthorNumber", sourceContributingAuthorNumber);
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
            return sourceContributionStatus;
        }

        int sourceContributingAuthorNumber() {
            return sourceContributingAuthorNumber;
        }

        String authorState() {
            return authorState;
        }

        int authorNumber() {
            return authorNumber;
        }

        boolean isSourceContributing() {
            return "contributing".equalsIgnoreCase(sourceContributionStatus);
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

        private static ObjectNode createExpressionInput(String name, String email) {
            var lcName = StringUtils.defaultString(name).toLowerCase();
            var lcEmail = StringUtils.defaultString(email).toLowerCase();
            var lcEmailDomain = StringUtils.substringAfter(lcEmail, "@");
            var lcEmailName = StringUtils.substringBefore(lcEmail, "@");
            var cleanName = lcName.replaceAll("[^a-z]", "");
            var cleanEmailName = lcEmailName.replaceAll("[^a-z0-9]", "");
            if ( !cleanEmailName.matches("[0-9]+") ) {
                cleanEmailName = cleanEmailName.replaceAll("^[0-9]+", "");
            }
            return JsonHelper.getObjectMapper().createObjectNode()
                    .put("name", StringUtils.defaultString(name))
                    .put("email", StringUtils.defaultString(email))
                    .put("lcName", lcName)
                    .put("lcEmail", lcEmail)
                    .put("lcEmailDomain", lcEmailDomain)
                    .put("lcEmailName", lcEmailName)
                    .put("cleanName", cleanName)
                    .put("cleanEmailName", cleanEmailName);
        }

        private static int parseInt(String value, int defaultValue) {
            try {
                return Integer.parseInt(StringUtils.defaultIfBlank(value, String.valueOf(defaultValue)));
            } catch ( NumberFormatException e ) {
                return defaultValue;
            }
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
