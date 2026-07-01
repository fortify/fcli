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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.license.ncd_report.reader.NcdReportReader;
import com.fortify.cli.license.ncd_report.validator.NcdReportValidator;
import com.fortify.cli.license.ncd_report.writer.NcdReportContributorsCsvSchema;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "update-contributor-status", aliases = {"ucs"})
public final class NcdReportUpdateContributorStatusCommand extends AbstractRunnableCommand {
    private static final CsvMapper CSV_MAPPER = new CsvMapper();
    private static final ObjectMapper JSON_MAPPER = JsonHelper.getObjectMapper();
    private static final ObjectMapper YAML_MAPPER = createYamlMapper();
    private static final Set<String> VALID_OVERRIDE_STATUSES = Set.of("contributing", "duplicate", "ignored");
    private static final double DEFAULT_MIN_CONFIDENCE = 0.8;

    @Option(names = {"-r", "--report"}, required = true)
    private Path reportPath;

    @Option(names = {"-c", "--contributors"})
    private Path contributorsPath;

    @Option(names = {"--min-confidence"}, defaultValue = "" + DEFAULT_MIN_CONFIDENCE)
    private double minConfidence;

    @Override
    public Integer call() {
        ObjectNode summary = null;
        int recordsProcessed = 0;
        int updatesApplied = 0;
        
        try ( var reader = new NcdReportReader(reportPath) ) {
            var checksumErrors = NcdReportValidator.validateChecksums(reader);
            if ( !checksumErrors.isEmpty() ) {
                throw new FcliSimpleException("Report integrity check failed:\n%s", String.join("\n", checksumErrors));
            }

            var updates = readUpdateFile();
            var contributors = readContributors(reader);
            var updateResult = applyUpdates(updates, contributors);
            recordsProcessed = updateResult.recordsProcessed();
            updatesApplied = updateResult.updatesApplied();
            var warnings = updateResult.warnings();
            synchronizeContributionFields(contributors, warnings);

            if ( !warnings.isEmpty() ) {
                System.err.println("Warnings during update:");
                warnings.forEach(w -> System.err.println("  - " + w));
            }

            summary = rewriteContributorsAndSummaryAndChecksums(reader, contributors);
        }
        
        System.out.println(String.format(
                "Processed %d update record(s), applied %d update(s) to report %s",
                recordsProcessed,
                updatesApplied,
                reportPath));
        System.out.println("Summary:");
        System.out.print(asYaml(summary));
        
        return 0;
    }

    // -------------------------------------------------------------------------
    // Reading update input
    // -------------------------------------------------------------------------

    private List<Map<String, String>> readUpdateFile() {
        try {
            var content = contributorsPath == null
                    ? new String(System.in.readAllBytes(), StandardCharsets.UTF_8)
                    : Files.readString(contributorsPath);
            if ( StringUtils.isBlank(content) ) {
                return List.of();
            }
            return switch ( detectInputFormat(content) ) {
            case JSON -> readStructuredUpdates(JSON_MAPPER, content, "JSON");
            case YAML -> readStructuredUpdates(YAML_MAPPER, content, "YAML");
            case CSV  -> readCsvUpdates(content);
            };
        } catch ( Exception e ) {
            throw new FcliSimpleException("Error reading contributor updates from %s:\n\tMessage: %s", getContributorsSource(), e.getMessage());
        }
    }

    private InputFormat detectInputFormat(String content) {
        var path = contributorsPath == null ? "" : contributorsPath.getFileName().toString().toLowerCase();
        if ( path.endsWith(".json") )            { return InputFormat.JSON; }
        if ( path.endsWith(".yaml") || path.endsWith(".yml") ) { return InputFormat.YAML; }
        if ( path.endsWith(".csv") )             { return InputFormat.CSV; }
        var trimmed = content.stripLeading();
        if ( trimmed.startsWith("{") || trimmed.startsWith("[") ) { return InputFormat.JSON; }
        if ( trimmed.startsWith("-") || trimmed.startsWith("---") ) { return InputFormat.YAML; }
        var firstLine = trimmed.lines().findFirst().orElse("").trim();
        if ( firstLine.matches("[A-Za-z0-9_-]+\\s*:.*") ) { return InputFormat.YAML; }
        return InputFormat.CSV;
    }

    private List<Map<String, String>> readCsvUpdates(String content) throws Exception {
        var schema = CsvSchema.emptySchema().withHeader();
        MappingIterator<Map<String, String>> it = CSV_MAPPER
                .readerFor(new TypeReference<Map<String, String>>() {})
                .with(schema)
                .readValues(content);
        var result = new ArrayList<Map<String, String>>();
        while ( it.hasNext() ) {
            var row = it.next();
            if ( !row.values().stream().allMatch(StringUtils::isBlank) ) {
                result.add(row);
            }
        }
        return result;
    }

    private List<Map<String, String>> readStructuredUpdates(ObjectMapper mapper, String content, String fmt) throws Exception {
        var root = mapper.readTree(content);
        if ( root == null || root.isNull() ) { return List.of(); }
        if ( root.isObject() ) { return List.of(toStringMap(root, fmt)); }
        if ( !root.isArray() ) {
            throw new FcliSimpleException("%s contributor updates must be an object or array of objects", fmt);
        }
        var result = new ArrayList<Map<String, String>>();
        for ( var node : root ) { result.add(toStringMap(node, fmt)); }
        return result;
    }

    private Map<String, String> toStringMap(JsonNode node, String fmt) {
        if ( !node.isObject() ) {
            throw new FcliSimpleException("%s contributor updates must contain only objects", fmt);
        }
        var result = new LinkedHashMap<String, String>();
        node.fields().forEachRemaining(e -> result.put(e.getKey(), jsonValueToString(e.getValue())));
        return result;
    }

    private String jsonValueToString(JsonNode node) {
        if ( node == null || node.isNull() ) { return ""; }
        return node.isValueNode() ? node.asText() : node.toString();
    }

    private String getContributorsSource() {
        return contributorsPath == null ? "stdin" : contributorsPath.toString();
    }

    // -------------------------------------------------------------------------
    // Applying updates
    // -------------------------------------------------------------------------

    private List<Map<String, String>> readContributors(NcdReportReader reader) {
        return reader.readContributors();
    }

    private UpdateApplicationResult applyUpdates(List<Map<String, String>> updates, List<Map<String, String>> contributors) {
        var warnings = new ArrayList<String>();
        int updatesApplied = 0;
        var byAuthorId = contributors.stream()
                .collect(Collectors.groupingBy(c -> c.getOrDefault(NcdReportContributorsCsvSchema.AUTHOR_ID, "")));

        for ( var update : updates ) {
            if ( applyUpdate(update, byAuthorId, warnings) ) {
                updatesApplied++;
            }
        }
        return new UpdateApplicationResult(updates.size(), updatesApplied, warnings);
    }

    private boolean applyUpdate(Map<String, String> update, Map<String, List<Map<String, String>>> byAuthorId, List<String> warnings) {
        var authorId = StringUtils.defaultString(update.get(NcdReportContributorsCsvSchema.AUTHOR_ID)).trim();
        if ( StringUtils.isBlank(authorId) ) {
            warnings.add("Update row missing authorId; skipping");
            return false;
        }
        var targets = byAuthorId.get(authorId);
        if ( targets == null ) {
            warnings.add(String.format("authorId %s: not found in report; skipping", authorId));
            return false;
        }
        if ( !validateUpdateRow(authorId, update, byAuthorId, warnings) ) {
            return false;
        }

        boolean hasAppliedFields = false;
        for ( var contributor : targets ) {
            if ( applyFieldsToContributor(authorId, update, contributor, targets.size(), warnings) ) {
                hasAppliedFields = true;
            }
        }
        if ( !hasAppliedFields ) {
            warnings.add(String.format("authorId %s: no updatable fields found; skipping", authorId));
        }
        return hasAppliedFields;
    }

    /**
     * Cross-field consistency validation for an update row.
     * Returns false if the entire row should be skipped.
     */
    private boolean validateUpdateRow(String authorId, Map<String, String> update,
            Map<String, List<Map<String, String>>> byAuthorId, List<String> warnings) {
        var overrideStatus = StringUtils.defaultString(update.get(NcdReportContributorsCsvSchema.OVERRIDE_STATUS)).trim();
        var duplicateOf    = StringUtils.defaultString(update.get(NcdReportContributorsCsvSchema.DUPLICATE_OF)).trim();
        var confidence     = StringUtils.defaultString(update.get(NcdReportContributorsCsvSchema.OVERRIDE_STATUS_CONFIDENCE)).trim();

        // Validate confidence threshold
        if ( StringUtils.isNotBlank(confidence) ) {
            try {
                double val = Double.parseDouble(confidence);
                if ( val < minConfidence ) {
                    warnings.add(String.format(
                            "authorId %s: confidence %.2f is below minimum %.2f; skipping",
                            authorId, val, minConfidence));
                    return false;
                }
            } catch ( NumberFormatException e ) {
                warnings.add(String.format("authorId %s: invalid confidence value '%s'; skipping", authorId, confidence));
                return false;
            }
        }

        // Validate overrideStatus value
        if ( StringUtils.isNotBlank(overrideStatus) && !VALID_OVERRIDE_STATUSES.contains(overrideStatus) ) {
            warnings.add(String.format("authorId %s: unknown overrideStatus '%s'; skipping", authorId, overrideStatus));
            return false;
        }

        // duplicate status requires duplicateOf
        if ( "duplicate".equals(overrideStatus) && StringUtils.isBlank(duplicateOf) ) {
            warnings.add(String.format("authorId %s: overrideStatus 'duplicate' requires a non-blank duplicateOf; skipping", authorId));
            return false;
        }

        // non-duplicate status must not set duplicateOf
        if ( StringUtils.isNotBlank(duplicateOf) && StringUtils.isNotBlank(overrideStatus)
                && !"duplicate".equals(overrideStatus) ) {
            warnings.add(String.format(
                    "authorId %s: duplicateOf set but overrideStatus is '%s' (not 'duplicate'); skipping",
                    authorId, overrideStatus));
            return false;
        }

        // duplicateOf must reference a known authorId
        if ( StringUtils.isNotBlank(duplicateOf) ) {
            if ( !byAuthorId.containsKey(duplicateOf) ) {
                warnings.add(String.format("authorId %s: duplicateOf '%s' references unknown authorId; skipping", authorId, duplicateOf));
                return false;
            }
            if ( authorId.equals(duplicateOf) ) {
                // Self-reference is silently ignored (can arise from lsc roundtrip)
                return true;
            }
        }

        return true;
    }

    private boolean applyFieldsToContributor(String authorId, Map<String, String> update, Map<String, String> contributor,
            int targetCount, List<String> warnings) {
        boolean hasAppliedFields = false;
        for ( var entry : update.entrySet() ) {
            var field = entry.getKey();
            var value = StringUtils.defaultString(entry.getValue()).trim();

            if ( StringUtils.isBlank(field) || field.equals(NcdReportContributorsCsvSchema.AUTHOR_ID) ) {
                continue;
            }
            if ( NcdReportContributorsCsvSchema.IMMUTABLE_FIELDS.contains(field) ) {
                // Only warn on immutable mismatches when authorId uniquely identifies a single row;
                // multiple rows share the same authorId when they are aliases of the same person.
                if ( targetCount == 1 ) {
                    warnIfImmutableMismatch(authorId, field, value, contributor, warnings);
                }
                continue;
            }
            if ( !NcdReportContributorsCsvSchema.UPDATABLE_FIELDS.contains(field) ) {
                warnings.add(String.format("authorId %s: unknown field '%s'; ignoring", authorId, field));
                continue;
            }
            contributor.put(field, value);
            hasAppliedFields = true;
        }
        return hasAppliedFields;
    }

    private void warnIfImmutableMismatch(String authorId, String field, String value,
            Map<String, String> contributor, List<String> warnings) {
        if ( StringUtils.isBlank(value) ) { return; }
        var existing = StringUtils.defaultString(contributor.get(field));
        if ( !existing.equals(value) ) {
            warnings.add(String.format(
                    "authorId %s: immutable field '%s' in update differs from report value; ignoring",
                    authorId, field));
        }
    }

    private void synchronizeContributionFields(List<Map<String, String>> contributors, List<String> warnings) {
        applyOverrideStatusToContributionStatus(contributors);
        recalculateContributingAuthorNumbers(contributors, warnings);
    }

    private void applyOverrideStatusToContributionStatus(List<Map<String, String>> contributors) {
        for ( var contributor : contributors ) {
            var overrideStatus = StringUtils.defaultString(
                    contributor.get(NcdReportContributorsCsvSchema.OVERRIDE_STATUS)).trim();
            if ( StringUtils.isBlank(overrideStatus) ) {
                continue;
            }
            contributor.put(NcdReportContributorsCsvSchema.CONTRIBUTION_STATUS, overrideStatus);
            if ( !"duplicate".equals(overrideStatus) ) {
                contributor.put(NcdReportContributorsCsvSchema.DUPLICATE_OF, "");
            }
        }
    }

    private void recalculateContributingAuthorNumbers(List<Map<String, String>> contributors, List<String> warnings) {
        var byAuthorId = contributors.stream()
                .filter(c -> StringUtils.isNotBlank(c.get(NcdReportContributorsCsvSchema.AUTHOR_ID)))
                .collect(Collectors.toMap(
                        c -> c.get(NcdReportContributorsCsvSchema.AUTHOR_ID),
                        c -> c,
                        (left, right) -> left,
                        LinkedHashMap::new));

        for ( var contributor : contributors ) {
            contributor.put(NcdReportContributorsCsvSchema.CONTRIBUTING_AUTHOR_NUMBER, "-1");
        }

        var contributing = contributors.stream()
                .filter(c -> "contributing".equals(normalizeStatus(c)))
                .sorted((left, right) -> StringUtils.defaultString(left.get(NcdReportContributorsCsvSchema.AUTHOR_NAME))
                        .compareToIgnoreCase(StringUtils.defaultString(right.get(NcdReportContributorsCsvSchema.AUTHOR_NAME))))
                .toList();
        int contributingAuthorNumber = 1;
        for ( var contributor : contributing ) {
            contributor.put(NcdReportContributorsCsvSchema.CONTRIBUTING_AUTHOR_NUMBER,
                    String.valueOf(contributingAuthorNumber++));
        }

        for ( var contributor : contributors ) {
            if ( !"duplicate".equals(normalizeStatus(contributor)) ) {
                continue;
            }
            var authorId = StringUtils.defaultString(contributor.get(NcdReportContributorsCsvSchema.AUTHOR_ID));
            var duplicateOf = StringUtils.defaultString(contributor.get(NcdReportContributorsCsvSchema.DUPLICATE_OF)).trim();
            if ( StringUtils.isBlank(duplicateOf) ) {
                warnings.add(String.format("authorId %s: duplicate status has blank duplicateOf; leaving contributingAuthorNumber as -1", authorId));
                continue;
            }

            var representative = resolveContributingRepresentative(duplicateOf, byAuthorId, new HashSet<>());
            if ( representative == null ) {
                warnings.add(String.format("authorId %s: duplicateOf '%s' does not resolve to a contributing author; leaving contributingAuthorNumber as -1",
                        authorId, duplicateOf));
                continue;
            }

            contributor.put(NcdReportContributorsCsvSchema.CONTRIBUTING_AUTHOR_NUMBER,
                    StringUtils.defaultString(representative.get(NcdReportContributorsCsvSchema.CONTRIBUTING_AUTHOR_NUMBER), "-1"));
        }
    }

    private Map<String, String> resolveContributingRepresentative(String authorId,
            Map<String, Map<String, String>> byAuthorId, Set<String> seenAuthorIds)
    {
        if ( !seenAuthorIds.add(authorId) ) {
            return null;
        }
        var contributor = byAuthorId.get(authorId);
        if ( contributor == null ) {
            return null;
        }
        var status = normalizeStatus(contributor);
        if ( "contributing".equals(status) ) {
            return contributor;
        }
        if ( "duplicate".equals(status) ) {
            var duplicateOf = StringUtils.defaultString(contributor.get(NcdReportContributorsCsvSchema.DUPLICATE_OF)).trim();
            if ( StringUtils.isBlank(duplicateOf) ) {
                return null;
            }
            return resolveContributingRepresentative(duplicateOf, byAuthorId, seenAuthorIds);
        }
        return null;
    }

    private String normalizeStatus(Map<String, String> contributor) {
        return StringUtils.defaultString(contributor.get(NcdReportContributorsCsvSchema.CONTRIBUTION_STATUS)).trim().toLowerCase();
    }

    // -------------------------------------------------------------------------
    // Writing back
    // -------------------------------------------------------------------------

    private ObjectNode rewriteContributorsAndSummaryAndChecksums(NcdReportReader reader, List<Map<String, String>> contributors) {
        var entryPath = reader.entryPath("contributors.csv");
        try {
            var sortedContributors = NcdReportContributorsCsvSchema.sortByAuthorNameAndStatus(
                contributors.stream()
                    .map(row -> JSON_MAPPER.convertValue(row, ObjectNode.class))
                    .toList());
            var presentColumns = contributors.stream()
                    .flatMap(map -> map.keySet().stream())
                    .collect(Collectors.toSet());
            var csvSchema = NcdReportContributorsCsvSchema.buildSchema(presentColumns);
            var outputColumns = NcdReportContributorsCsvSchema.getOutputColumns().stream()
                    .filter(presentColumns::contains)
                    .toList();
            var writableContributors = sortedContributors.stream()
                .map(row -> JSON_MAPPER.convertValue(row, new TypeReference<Map<String, String>>() {}))
                    .map(row -> {
                        Map<String, String> writableRow = new java.util.LinkedHashMap<>();
                        outputColumns.forEach(column -> writableRow.put(column, row.get(column)));
                        return writableRow;
                    })
                    .toList();
            var csv = CSV_MAPPER.writer(csvSchema).writeValueAsString(writableContributors);
            Files.write(entryPath, csv.getBytes(StandardCharsets.UTF_8));
            updateChecksum(reader, "contributors.csv");

            var summary = updateSummary(reader, writableContributors);
            updateChecksum(reader, "summary.txt");
            return summary;
        } catch ( Exception e ) {
            throw new FcliTechnicalException(String.format("Error updating contributors.csv in %s", reader.getReportPath()), e);
        }
    }

    private ObjectNode updateSummary(NcdReportReader reader, List<Map<String, String>> contributors) {
        var summaryPath = reader.entryPath("summary.txt");
        var summary = reader.readSummary().deepCopy();

        int total = contributors.size();
        int ignored = (int) contributors.stream().filter(c -> "ignored".equals(normalizeStatus(c))).count();
        int duplicate = (int) contributors.stream().filter(c -> "duplicate".equals(normalizeStatus(c))).count();
        int contributing = (int) contributors.stream().filter(c -> "contributing".equals(normalizeStatus(c))).count();
        int nonIgnored = total - ignored;

        summary.set("authorCount", JsonHelper.getObjectMapper().createObjectNode()
                .put("total", total)
                .put("contributing", contributing)
                .put("ignored", ignored)
                .put("nonIgnored", nonIgnored)
                .put("duplicate", duplicate));
        try {
            Files.write(summaryPath, YAML_MAPPER.writeValueAsBytes(summary));
        } catch ( Exception e ) {
            throw new FcliTechnicalException(String.format("Error updating summary.txt in %s", reader.getReportPath()), e);
        }
        return summary;
    }

    private String asYaml(ObjectNode summary) {
        try {
            return YAML_MAPPER.writeValueAsString(summary);
        } catch ( Exception e ) {
            throw new FcliTechnicalException("Error formatting summary output", e);
        }
    }

    private void updateChecksum(NcdReportReader reader, String entryName) {
        var checksumsPath = reader.entryPath("checksums.sha256");
        try {
            var lines = Files.readAllLines(checksumsPath, StandardCharsets.UTF_8);
            var updated = new ArrayList<String>();
            var entryChecksum = NcdReportValidator.sha256(reader.entryPath(entryName));
            boolean found = false;
            for ( var line : lines ) {
                var parts = line.split("\\s+", 2);
                if ( parts.length >= 2 ) {
                    var filename = parts[1].startsWith("*") ? parts[1].substring(1) : parts[1];
                    if ( filename.equals(entryName) ) {
                        updated.add(String.format("%s %s", entryChecksum, entryName));
                        found = true;
                    } else {
                        updated.add(line);
                    }
                } else {
                    updated.add(line);
                }
            }
            if ( !found ) {
                updated.add(String.format("%s %s", entryChecksum, entryName));
            }
            Files.write(checksumsPath, updated, StandardCharsets.UTF_8);
        } catch ( Exception e ) {
            throw new FcliTechnicalException(String.format("Error updating checksums in %s", reader.getReportPath()), e);
        }
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static ObjectMapper createYamlMapper() {
        var mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    private enum InputFormat {
        CSV, JSON, YAML
    }

    private record UpdateApplicationResult(int recordsProcessed, int updatesApplied, List<String> warnings) {}
}
