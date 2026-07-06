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
package com.fortify.cli.license.ncd_report.generator.mock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.license.ncd_report.collector.INcdReportRepositoryBranchCommitCollector;
import com.fortify.cli.license.ncd_report.collector.NcdReportContext;
import com.fortify.cli.license.ncd_report.config.NcdReportMockSourceConfig;
import com.fortify.cli.license.ncd_report.descriptor.INcdReportRepositoryDescriptor;
import com.fortify.cli.license.ncd_report.descriptor.NcdReportBranchCommitDescriptor;
import com.fortify.cli.license.ncd_report.generator.AbstractNcdReportResultsGenerator;

/**
 * Results generator for mock SCM source, useful for testing NCD report functionality.
 * Generates synthetic repositories, branches, and commits with configurable parameters.
 * Supports both built-in realistic authors and loading from external JSON/CSV files.
 */
public class NcdReportMockResultsGenerator extends AbstractNcdReportResultsGenerator<NcdReportMockSourceConfig> {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private List<MockAuthorData> authors = new ArrayList<>();
    
    public NcdReportMockResultsGenerator(NcdReportMockSourceConfig sourceConfig, NcdReportContext reportContext) {
        super(sourceConfig, reportContext);
        initializeAuthors();
    }

    /**
     * Initialize authors from dataFile if configured, otherwise use built-in realistic data.
     */
    private void initializeAuthors() {
        if ( sourceConfig().getDataFile().isPresent() ) {
            String dataFilePath = sourceConfig().getDataFile().get();
            try {
                loadAuthorsFromFile(dataFilePath);
            } catch ( Exception e ) {
                reportContext().logger().warn("Failed to load mock data from " + dataFilePath + ", falling back to built-in data", e);
                authors = new ArrayList<>(MockAuthorData.getAllRealisticAuthors());
            }
        } else {
            authors = new ArrayList<>(MockAuthorData.getAllRealisticAuthors());
        }
    }
    
    /**
     * Load authors from JSON, YAML, or CSV file.
     */
    private void loadAuthorsFromFile(String dataFilePath) throws Exception {
        var file = new File(dataFilePath);
        if ( !file.exists() ) {
            throw new FcliSimpleException("Data file not found: " + dataFilePath);
        }

        var lcDataFilePath = dataFilePath.toLowerCase();
        if ( lcDataFilePath.endsWith(".json") ) {
            loadAuthorsFromStructuredFile(file, JSON_MAPPER, "JSON");
        } else if ( lcDataFilePath.endsWith(".yaml") || lcDataFilePath.endsWith(".yml") ) {
            loadAuthorsFromStructuredFile(file, YAML_MAPPER, "YAML");
        } else if ( lcDataFilePath.endsWith(".csv") ) {
            loadAuthorsFromCsv(file);
        } else {
            throw new FcliSimpleException("Unsupported data file format: " + dataFilePath + 
                ". Supported formats: JSON, YAML, CSV");
        }
    }

    /**
     * Load authors from JSON or YAML file. Expects array of objects with name and email fields,
     * or an object containing an authors array.
     */
    private void loadAuthorsFromStructuredFile(File file, ObjectMapper mapper, String formatName) throws IOException {
        JsonNode root = mapper.readTree(file);

        if ( root.isArray() ) {
            addAuthorsFromArray(root);
        } else if ( root.isObject() ) {
            JsonNode authorsNode = root.get("authors");
            if ( authorsNode != null && authorsNode.isArray() ) {
                addAuthorsFromArray(authorsNode);
            }
        }

        if ( authors.isEmpty() ) {
            throw new FcliSimpleException("No valid authors found in %s file: %s", formatName, file.getPath());
        }
    }

    private void addAuthorsFromArray(JsonNode authorsNode) {
        for ( JsonNode node : authorsNode ) {
            String name = node.has("name") ? node.get("name").asText() : "";
            String email = node.has("email") ? node.get("email").asText() : "";
            if ( !name.isEmpty() && !email.isEmpty() ) {
                authors.add(new MockAuthorData(name, email));
            }
        }
    }
    
    /**
     * Load authors from CSV file. Expects columns: name, email.
     */
    private void loadAuthorsFromCsv(File file) throws IOException {
        String csvContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        var schema = CsvSchema.emptySchema().withHeader();
        CsvMapper csvMapper = new CsvMapper();
        
        MappingIterator<Map<String, String>> iterator = csvMapper
                .readerFor(new TypeReference<Map<String, String>>() {})
                .with(schema)
                .readValues(csvContent);
        
        while ( iterator.hasNext() ) {
            var row = iterator.next();
            String name = row.get("name");
            String email = row.get("email");
            if ( name != null && !name.trim().isEmpty() && email != null && !email.trim().isEmpty() ) {
                authors.add(new MockAuthorData(name.trim(), email.trim()));
            }
        }
        
        if ( authors.isEmpty() ) {
            throw new FcliSimpleException("No valid authors found in CSV file: " + file.getPath());
        }
    }

    @Override
    protected void generateResults() {
        var reportContext = reportContext();
        var repositoryProcessor = reportContext.repositoryProcessor();
        var endDateTime = reportContext.reportConfig().getCommitEndDateTime();
        var startDateTime = reportContext.reportConfig().getCommitStartDateTime();

        var activeRepositoryCount = Math.max(0, sourceConfig().getActiveRepositoryCount());
        var dormantOverlappingRepositoryCount = Math.max(0, sourceConfig().getDormantOverlappingRepositoryCount());
        var dormantNonOverlappingRepositoryCount = Math.max(0, sourceConfig().getDormantNonOverlappingRepositoryCount());
        var totalRepositoryCount = activeRepositoryCount + dormantOverlappingRepositoryCount + dormantNonOverlappingRepositoryCount;

        // Generate active repositories
        for ( int i = 1; i <= activeRepositoryCount; i++ ) {
            final int repoIndex = i;
            var repoName = "mock-repo-" + repoIndex;
            var repoUrl = "https://mock.example.com/repos/" + repoName;
            var repoDescriptor = new MockNcdReportRepositoryDescriptor(repoName, repoUrl, "public", false);
            
            repositoryProcessor.processRepository(
                sourceConfig(),
                repoDescriptor,
                (repo, branchCollector) -> generateActiveCommitDataForRepository(branchCollector, repoIndex, repoDescriptor, startDateTime, endDateTime)
            );
        }

        // Generate dormant repositories with overlapping contributors (single dormant commit/author)
        int dormantOrdinal = 0;
        int firstDormantIndex = activeRepositoryCount + 1;
        int lastDormantOverlappingIndex = activeRepositoryCount + dormantOverlappingRepositoryCount;
        for ( int i = firstDormantIndex; i <= lastDormantOverlappingIndex; i++ ) {
            final int repoIndex = i;
            final int currentDormantOrdinal = ++dormantOrdinal;
            var repoName = "mock-repo-" + repoIndex;
            var repoUrl = "https://mock.example.com/repos/" + repoName;
            var repoDescriptor = new MockNcdReportRepositoryDescriptor(repoName, repoUrl, "public", false);

            repositoryProcessor.processRepository(
                sourceConfig(),
                repoDescriptor,
                (repo, branchCollector) -> generateDormantCommitDataForRepository(
                        branchCollector, repoDescriptor, startDateTime, true, currentDormantOrdinal)
            );
        }

        // Generate dormant repositories with non-overlapping contributors (single dormant commit/author)
        for ( int i = lastDormantOverlappingIndex + 1; i <= totalRepositoryCount; i++ ) {
            final int repoIndex = i;
            final int currentDormantOrdinal = ++dormantOrdinal;
            var repoName = "mock-repo-" + repoIndex;
            var repoUrl = "https://mock.example.com/repos/" + repoName;
            var repoDescriptor = new MockNcdReportRepositoryDescriptor(repoName, repoUrl, "public", false);

            repositoryProcessor.processRepository(
                sourceConfig(),
                repoDescriptor,
                (repo, branchCollector) -> generateDormantCommitDataForRepository(
                        branchCollector, repoDescriptor, startDateTime, false, currentDormantOrdinal)
            );
        }
    }
    
    private void generateActiveCommitDataForRepository(
            INcdReportRepositoryBranchCommitCollector branchCollector,
            int repoIndex,
            INcdReportRepositoryDescriptor repositoryDescriptor,
            OffsetDateTime reportStartDateTime,
            OffsetDateTime reportEndDateTime) {
        
        var authorsPerRepo = sourceConfig().getAuthorsPerRepository();
        var commitsPerAuthor = sourceConfig().getCommitsPerAuthor();
        var branchDescriptor = new MockNcdReportBranchDescriptor("main", "abc123");
        
        // Generate authors and commits using realistic data
        int authorIndex = ((repoIndex - 1) * authorsPerRepo);
        for ( int a = 0; a < authorsPerRepo; a++ ) {
            // Use realistic authors, cycling through available authors
            MockAuthorData authorData = authors.get((authorIndex + a) % authors.size());
            var author = new MockNcdReportAuthorDescriptor(authorData.getName(), authorData.getEmail());
            
            // Generate commits for this author spread across the report window + some before
            for ( int c = 1; c <= commitsPerAuthor; c++ ) {
                // Distribute commits: some before the window, most within the window
                var daysOffset = (c % 2 == 0) ? -120 + (c / 2) : -30 + (c / 2);
                var commitDateTime = reportEndDateTime.plusDays(daysOffset);
                
                var commitSha = String.format("%040x", (long)repoIndex * 10000 + (long)a * 100 + c);
                var commitDescriptor = new MockNcdReportCommitDescriptor(commitSha, commitDateTime);
                
                // Create the full branch commit descriptor
                var dormant = commitDateTime.isBefore(reportStartDateTime);
                var branchCommitDescriptor = new NcdReportBranchCommitDescriptor(
                    repositoryDescriptor,
                    branchDescriptor,
                    commitDescriptor,
                    author,
                    dormant
                );
                
                branchCollector.reportBranchCommit(branchCommitDescriptor);
            }
        }
    }

    private void generateDormantCommitDataForRepository(
            INcdReportRepositoryBranchCommitCollector branchCollector,
            INcdReportRepositoryDescriptor repositoryDescriptor,
            OffsetDateTime reportStartDateTime,
            boolean overlapWithActiveRepositories,
            int dormantOrdinal) {
        var branchDescriptor = new MockNcdReportBranchDescriptor("main", "abc123");
        var authorData = selectDormantRepositoryAuthorData(overlapWithActiveRepositories, dormantOrdinal);
        var author = new MockNcdReportAuthorDescriptor(authorData.getName(), authorData.getEmail());
        var commitDateTime = reportStartDateTime.minusDays(1);
        var commitSha = String.format("%040x", ((long) dormantOrdinal * 10000) + 1);
        var commitDescriptor = new MockNcdReportCommitDescriptor(commitSha, commitDateTime);

        var branchCommitDescriptor = new NcdReportBranchCommitDescriptor(
                repositoryDescriptor,
                branchDescriptor,
                commitDescriptor,
                author,
                true);
        branchCollector.reportBranchCommit(branchCommitDescriptor);
    }

    private MockAuthorData selectDormantRepositoryAuthorData(boolean overlapWithActiveRepositories, int dormantOrdinal) {
        if ( overlapWithActiveRepositories ) {
            // Re-use the first active author to force overlap and test active-wins semantics.
            return authors.get(0);
        }

        // Use synthetic authors by default to keep dormant-only contributors visible in top-level results.
        var suffix = toAlphabeticSuffix(dormantOrdinal);
        var name = String.format("Dormant Repo User %s", suffix);
        var email = String.format("dormant.repo.user.%s@example.com", suffix.toLowerCase());
        return new MockAuthorData(name, email);
    }

    private String toAlphabeticSuffix(int ordinal) {
        int value = Math.max(1, ordinal);
        var result = new StringBuilder();
        while ( value > 0 ) {
            value--;
            result.insert(0, (char) ('A' + (value % 26)));
            value = value / 26;
        }
        return result.toString();
    }
    
    @Override
    protected String getType() {
        return "mock";
    }
}
