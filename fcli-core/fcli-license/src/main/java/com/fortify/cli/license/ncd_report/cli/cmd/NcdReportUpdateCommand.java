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

@Command(name = "update")
public final class NcdReportUpdateCommand extends AbstractRunnableCommand {
    private static final CsvMapper CSV_MAPPER = new CsvMapper();
    private static final ObjectMapper JSON_MAPPER = JsonHelper.getObjectMapper();
    private static final ObjectMapper YAML_MAPPER = createYamlMapper();
    private static final Set<String> VALID_OVERRIDDEN_STATUSES = Set.of("contributing", "duplicate", "ignored");

    @Option(names = {"-r", "--report"}, required = true)
    private Path reportPath;

    @Option(names = {"-c", "--contributors"})
    private Path contributorsPath;

    public enum OnUnknownAuthor {
        fail, warn, ignore
    }

    @Option(names = {"--on-unknown-author"}, defaultValue = "fail")
    private OnUnknownAuthor onUnknownAuthor;

    @Override
    public Integer call() {
        try ( var reader = new NcdReportReader(reportPath) ) {
            // Validate report integrity before making changes
            var checksumErrors = NcdReportValidator.validateChecksums(reader);
            if ( !checksumErrors.isEmpty() ) {
                throw new FcliSimpleException("Report integrity check failed:\n%s", String.join("\n", checksumErrors));
            }

            var updates = readUpdateFile();
            var contributors = readContributors(reader);
            var warnings = validateAndApplyUpdates(updates, contributors);
            
            if ( !warnings.isEmpty() ) {
                System.err.println("Warnings during update:");
                warnings.forEach(w -> System.err.println("  - " + w));
            }

            rewriteContributorsAndChecksums(reader, contributors);
            System.out.println(String.format("Successfully updated %d contributors in report %s", updates.size(), reportPath));
        }
        return 0;
    }

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
            case CSV -> readCsvUpdates(content);
            };
        } catch ( Exception e ) {
            throw new FcliSimpleException("Error reading contributor updates from %s:\n\tMessage: %s", getContributorsSource(), e.getMessage());
        }
    }

    private InputFormat detectInputFormat(String content) {
        var lowerCasePath = contributorsPath == null ? "" : contributorsPath.getFileName().toString().toLowerCase();
        if ( lowerCasePath.endsWith(".json") ) {
            return InputFormat.JSON;
        }
        if ( lowerCasePath.endsWith(".yaml") || lowerCasePath.endsWith(".yml") ) {
            return InputFormat.YAML;
        }
        if ( lowerCasePath.endsWith(".csv") ) {
            return InputFormat.CSV;
        }

        var trimmed = content.stripLeading();
        if ( trimmed.startsWith("{") || trimmed.startsWith("[") ) {
            return InputFormat.JSON;
        }
        if ( trimmed.startsWith("-") || trimmed.startsWith("---") ) {
            return InputFormat.YAML;
        }
        var firstLine = trimmed.lines().findFirst().orElse("").trim();
        if ( firstLine.matches("[A-Za-z0-9_-]+\\s*:.*") ) {
            return InputFormat.YAML;
        }
        return InputFormat.CSV;
    }

    private List<Map<String, String>> readCsvUpdates(String content) throws Exception {
        var schema = CsvSchema.emptySchema().withHeader();
        MappingIterator<Map<String, String>> iterator = CSV_MAPPER
                .readerFor(new TypeReference<Map<String, String>>() {})
                .with(schema)
                .readValues(content);
        var result = new ArrayList<Map<String, String>>();
        while ( iterator.hasNext() ) {
            var row = iterator.next();
            if ( row.values().stream().allMatch(StringUtils::isBlank) ) {
                continue;
            }
            result.add(row);
        }
        return result;
    }

    private List<Map<String, String>> readStructuredUpdates(ObjectMapper mapper, String content, String formatName) throws Exception {
        var rootNode = mapper.readTree(content);
        if ( rootNode == null || rootNode.isNull() ) {
            return List.of();
        }
        if ( rootNode.isObject() ) {
            return List.of(toStringMap(rootNode, formatName));
        }
        if ( !rootNode.isArray() ) {
            throw new FcliSimpleException("%s contributor updates must be an object or array of objects", formatName);
        }

        var result = new ArrayList<Map<String, String>>();
        for ( var node : rootNode ) {
            result.add(toStringMap(node, formatName));
        }
        return result;
    }

    private Map<String, String> toStringMap(JsonNode node, String formatName) {
        if ( !node.isObject() ) {
            throw new FcliSimpleException("%s contributor updates must contain only objects", formatName);
        }
        var result = new LinkedHashMap<String, String>();
        node.fields().forEachRemaining(entry -> result.put(entry.getKey(), jsonValueToString(entry.getValue())));
        return result;
    }

    private String jsonValueToString(JsonNode node) {
        if ( node == null || node.isNull() ) {
            return "";
        }
        return node.isValueNode() ? node.asText() : node.toString();
    }

    private String getContributorsSource() {
        return contributorsPath == null ? "stdin" : contributorsPath.toString();
    }

    private List<Map<String, String>> readContributors(NcdReportReader reader) {
        return reader.readContributors();
    }

    private List<String> validateAndApplyUpdates(List<Map<String, String>> updates, List<Map<String, String>> contributors) {
        var warnings = new ArrayList<String>();
        var contributersByAuthorId = contributors.stream()
                .collect(Collectors.groupingBy(c -> c.getOrDefault(NcdReportContributorsCsvSchema.AUTHOR_ID, "")));

        for ( var update : updates ) {
            var authorId = StringUtils.defaultString(update.get(NcdReportContributorsCsvSchema.AUTHOR_ID)).trim();
            if ( StringUtils.isBlank(authorId) ) {
                warnings.add("Update row missing authorId, skipping");
                continue;
            }

            var matchingContributors = contributersByAuthorId.get(authorId);
            if ( matchingContributors == null ) {
                String msg = String.format("Unknown authorId '%s' in update", authorId);
                if ( onUnknownAuthor == OnUnknownAuthor.fail ) {
                    throw new FcliSimpleException(msg);
                }
                if ( onUnknownAuthor == OnUnknownAuthor.warn ) {
                    warnings.add(msg);
                }
                continue;
            }

            for ( var contributor : matchingContributors ) {
                for ( var entry : update.entrySet() ) {
                    var field = entry.getKey();
                    var value = StringUtils.defaultString(entry.getValue()).trim();

                    if ( field.equals(NcdReportContributorsCsvSchema.AUTHOR_ID) || StringUtils.isBlank(field) ) {
                        continue;
                    }

                    if ( NcdReportContributorsCsvSchema.IMMUTABLE_FIELDS.contains(field) ) {
                        if ( matchingContributors.size() == 1 ) {
                            var existingValue = StringUtils.defaultString(contributor.get(field));
                            if ( !existingValue.equals(value) && !StringUtils.isBlank(value) ) {
                                warnings.add(String.format("authorId %s: immutable field '%s' in update mismatches report value; ignoring", authorId, field));
                            }
                        }
                        continue;
                    }

                    if ( !NcdReportContributorsCsvSchema.UPDATABLE_FIELDS.contains(field) ) {
                        warnings.add(String.format("authorId %s: unknown field '%s'; ignoring", authorId, field));
                        continue;
                    }

                    if ( NcdReportContributorsCsvSchema.OVERRIDDEN_STATUS.equals(field) ) {
                        if ( StringUtils.isBlank(value) ) {
                            contributor.put(field, "");
                            continue;
                        }
                        if ( !VALID_OVERRIDDEN_STATUSES.contains(value) ) {
                            warnings.add(String.format("authorId %s: invalid overriddenStatus '%s'; ignoring", authorId, value));
                            continue;
                        }
                    }

                    if ( NcdReportContributorsCsvSchema.AI_DUPLICATE_OF.equals(field) ) {
                        if ( !StringUtils.isBlank(value) && !contributersByAuthorId.containsKey(value) ) {
                            warnings.add(String.format("authorId %s: aiDuplicateOf references unknown authorId '%s'; ignoring", authorId, value));
                            continue;
                        }
                        if ( authorId.equals(value) ) {
                            warnings.add(String.format("authorId %s: aiDuplicateOf cannot reference self; ignoring", authorId));
                            continue;
                        }
                    }

                    contributor.put(field, value);
                }
            }
        }

        return warnings;
    }

    private void rewriteContributorsAndChecksums(NcdReportReader reader, List<Map<String, String>> contributors) {
        var reportPath = reader.getReportPath();
        var entryPath = reader.entryPath("contributors.csv");

        try {
            // Build schema with only the columns actually present in the data
            var presentColumns = contributors.isEmpty() ? Set.<String>of() : contributors.get(0).keySet();
            var csvSchema = NcdReportContributorsCsvSchema.buildSchema(presentColumns);

            var writer = CSV_MAPPER.writer(csvSchema);
            var csv = writer.writeValueAsString(contributors);
            Files.write(entryPath, csv.getBytes(StandardCharsets.UTF_8));

            updateChecksum(reader, "contributors.csv");
        } catch ( Exception e ) {
            throw new FcliTechnicalException(String.format("Error updating contributors.csv in %s", reportPath), e);
        }
    }

    private void updateChecksum(NcdReportReader reader, String entryName) {
        var reportPath = reader.getReportPath();
        var checksumsPath = reader.entryPath("checksums.sha256");

        try {
            var lines = Files.readAllLines(checksumsPath);
            var updated = new ArrayList<String>();
            var entryChecksum = NcdReportValidator.sha256(reader.entryPath(entryName));
            boolean found = false;

            for ( var line : lines ) {
                var parts = line.split("\\s+", 2);
                if ( parts.length >= 2 && parts[1].equals(entryName) ) {
                    updated.add(String.format("%s %s", entryChecksum, entryName));
                    found = true;
                } else {
                    updated.add(line);
                }
            }

            if ( !found ) {
                updated.add(String.format("%s %s", entryChecksum, entryName));
            }

            Files.write(checksumsPath, updated, StandardCharsets.UTF_8);
        } catch ( Exception e ) {
            throw new FcliTechnicalException(String.format("Error updating checksums in %s", reportPath), e);
        }
    }

    private static ObjectMapper createYamlMapper() {
        var mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    private enum InputFormat {
        CSV, JSON, YAML
    }
}
