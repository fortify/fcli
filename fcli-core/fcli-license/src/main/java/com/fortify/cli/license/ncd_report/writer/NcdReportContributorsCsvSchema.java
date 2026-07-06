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
package com.fortify.cli.license.ncd_report.writer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Centralized definition of NCD report contributors.csv column schema.
 * This ensures consistency across all commands that read or write contributors.csv.
 */
public final class NcdReportContributorsCsvSchema {
    // Core author identity fields (immutable, always present)
    public static final String AUTHOR_ID = "authorId";
    public static final String AUTHOR_NAME = "authorName";
    public static final String AUTHOR_EMAIL = "authorEmail";
    public static final String CLEAN_NAME = "cleanName";
    public static final String CLEAN_EMAIL_NAME = "cleanEmailName";

    // Author state fields (immutable, set during create/merge)
    public static final String AUTHOR_STATE = "authorState";
    public static final String AUTHOR_NUMBER = "authorNumber";

    // Contribution status fields (immutable, set during create/merge)
    public static final String CONTRIBUTION_STATUS = "contributionStatus";
    public static final String CONTRIBUTING_AUTHOR_NUMBER = "contributingAuthorNumber";

    // Source report fields (immutable, from merged reports only)
    public static final String SOURCE_REPORTS = "sourceReports";
    public static final String SOURCE_CONTRIBUTION_STATUS = "sourceContributionStatus";
    public static final String SOURCE_CONTRIBUTING_AUTHOR_NUMBER = "sourceContributingAuthorNumber";
    public static final String SOURCE_AUTHOR_ID = "sourceAuthorId";
    public static final String SOURCE_AUTHOR_STATE = "sourceAuthorState";
    public static final String SOURCE_AUTHOR_NUMBER = "sourceAuthorNumber";

    // Merged report fields (immutable, from merged reports only)
    public static final String MERGED_AUTHOR_ID = "mergedAuthorId";
    public static final String MERGED_AUTHOR_STATE = "mergedAuthorState";
    public static final String MERGED_AUTHOR_NUMBER = "mergedAuthorNumber";
    public static final String MERGED_CONTRIBUTION_STATUS = "mergedContributionStatus";
    public static final String MERGED_CONTRIBUTING_AUTHOR_NUMBER = "mergedContributingAuthorNumber";

    // Updatable override fields
    public static final String DUPLICATE_OF = "duplicateOf";
    public static final String OVERRIDE_STATUS = "overrideStatus";
    public static final String OVERRIDE_STATUS_CONFIDENCE = "overrideStatusConfidence";
    public static final String OVERRIDE_STATUS_NOTES = "overrideStatusNotes";

    // All immutable fields (identity, state, and source/merge semantics)
    public static final Set<String> IMMUTABLE_FIELDS = Set.of(
        AUTHOR_ID, AUTHOR_NAME, AUTHOR_EMAIL, CLEAN_NAME, CLEAN_EMAIL_NAME,
        AUTHOR_STATE, AUTHOR_NUMBER,
        CONTRIBUTION_STATUS, CONTRIBUTING_AUTHOR_NUMBER,
        SOURCE_REPORTS, SOURCE_CONTRIBUTION_STATUS, SOURCE_CONTRIBUTING_AUTHOR_NUMBER,
        SOURCE_AUTHOR_ID, SOURCE_AUTHOR_STATE, SOURCE_AUTHOR_NUMBER,
        MERGED_AUTHOR_ID, MERGED_AUTHOR_STATE, MERGED_AUTHOR_NUMBER,
        MERGED_CONTRIBUTION_STATUS, MERGED_CONTRIBUTING_AUTHOR_NUMBER
    );

    // All updatable fields
    public static final Set<String> UPDATABLE_FIELDS = Set.of(
        DUPLICATE_OF,
        OVERRIDE_STATUS,
        OVERRIDE_STATUS_CONFIDENCE,
        OVERRIDE_STATUS_NOTES
    );

    // Column order for CSV output
    private static final List<String> COLUMN_ORDER = Arrays.asList(
        AUTHOR_ID,
        AUTHOR_NAME,
        AUTHOR_EMAIL,
        CLEAN_NAME,
        CLEAN_EMAIL_NAME,
        AUTHOR_STATE,
        CONTRIBUTION_STATUS,
        SOURCE_REPORTS,
        SOURCE_CONTRIBUTION_STATUS,
        SOURCE_AUTHOR_ID,
        SOURCE_AUTHOR_STATE,
        MERGED_AUTHOR_ID,
        MERGED_AUTHOR_STATE,
        MERGED_CONTRIBUTION_STATUS,
        DUPLICATE_OF,
        OVERRIDE_STATUS_CONFIDENCE,
        OVERRIDE_STATUS_NOTES,
        OVERRIDE_STATUS
    );

    /**
     * Build a CsvSchema with all defined columns in the correct order.
     * Only includes columns that are actually present in the provided list.
     * @param presentColumns Set of column names that should be included in the schema
     * @return CsvSchema with header and specified columns
     */
    public static CsvSchema buildSchema(Set<String> presentColumns) {
        var builder = CsvSchema.builder();
        for ( var column : COLUMN_ORDER ) {
            if ( presentColumns.contains(column) ) {
                builder.addColumn(column);
            }
        }
        return builder.build().withUseHeader(true);
    }

    /**
     * Build a CsvSchema with all known columns in the correct order.
     * Used when writing a complete contributors.csv with all possible fields.
     * @return CsvSchema with header and all columns
     */
    public static CsvSchema buildFullSchema() {
        var builder = CsvSchema.builder();
        for ( var column : COLUMN_ORDER ) {
            builder.addColumn(column);
        }
        return builder.build().withUseHeader(true);
    }

    /**
     * Return the list of columns written to contributors.csv output.
     */
    public static List<String> getOutputColumns() {
        return COLUMN_ORDER;
    }

    /**
     * Sort contributors for consistent CSV output using a two-phase approach.
     * Phase 1: Collect and organize records by status (contributing, duplicate, ignored).
     * Phase 2: Build result by iterating contributing records (sorted by name),
     *          adding each record's duplicates immediately after, then all ignored records at end.
     * This sorting is necessary for legacy reports and improves readability.
     */
    public static List<ObjectNode> sortByAuthorNameAndStatus(List<ObjectNode> contributors) {
        var nameComparator = Comparator.comparing(
            (ObjectNode r) -> r.path(AUTHOR_NAME).asText("").toLowerCase());

        var contributing = new ArrayList<ObjectNode>();
        var duplicates = new ArrayList<ObjectNode>();
        var ignored = new ArrayList<ObjectNode>();
        splitByStatus(contributors, contributing, duplicates, ignored);

        var contributingByAuthorId = indexContributingByAuthorId(contributing);
        var contributingByNumber = indexContributingByNumber(contributing);
        var duplicateByRepId = new TreeMap<String, ArrayList<ObjectNode>>();
        var unmatchedDuplicates = new ArrayList<ObjectNode>();
        groupDuplicates(duplicates, duplicateByRepId, unmatchedDuplicates, contributingByAuthorId, contributingByNumber);

        // Sort each group by author name
        contributing.sort(nameComparator);
        duplicateByRepId.values().forEach(list -> list.sort(nameComparator));
        unmatchedDuplicates.sort(nameComparator);
        ignored.sort(nameComparator);

        // Phase 2: Build result: contributing + their duplicates + unmatched duplicates + ignored
        var result = new ArrayList<ObjectNode>();
        for ( var contribRecord : contributing ) {
            result.add(contribRecord);
            var repId = contribRecord.path(AUTHOR_ID).asText("");
            if ( duplicateByRepId.containsKey(repId) ) {
                result.addAll(duplicateByRepId.get(repId));
            }
        }
        result.addAll(unmatchedDuplicates);
        result.addAll(ignored);

        return result;
    }

    private static void splitByStatus(List<ObjectNode> contributors, List<ObjectNode> contributing,
            List<ObjectNode> duplicates, List<ObjectNode> ignored)
    {
        for ( var record : contributors ) {
            var status = record.path(CONTRIBUTION_STATUS).asText("").toLowerCase();
            if ( "contributing".equals(status) ) {
                contributing.add(record);
            } else if ( "duplicate".equals(status) ) {
                duplicates.add(record);
            } else if ( "ignored".equals(status) ) {
                ignored.add(record);
            }
        }
    }

    private static TreeMap<String, ObjectNode> indexContributingByAuthorId(List<ObjectNode> contributing) {
        var result = new TreeMap<String, ObjectNode>();
        for ( var record : contributing ) {
            var authorId = record.path(AUTHOR_ID).asText("").trim();
            if ( !authorId.isBlank() ) {
                result.put(authorId, record);
            }
        }
        return result;
    }

    private static TreeMap<Integer, ObjectNode> indexContributingByNumber(List<ObjectNode> contributing) {
        var result = new TreeMap<Integer, ObjectNode>();
        for ( var record : contributing ) {
            var number = parsePositiveInt(record.path(CONTRIBUTING_AUTHOR_NUMBER).asText(""));
            if ( number > 0 ) {
                result.putIfAbsent(number, record);
            }
        }
        return result;
    }

    private static void groupDuplicates(List<ObjectNode> duplicates, TreeMap<String, ArrayList<ObjectNode>> duplicateByRepId,
            List<ObjectNode> unmatchedDuplicates, TreeMap<String, ObjectNode> contributingByAuthorId,
            TreeMap<Integer, ObjectNode> contributingByNumber)
    {
        for ( var duplicate : duplicates ) {
            var representativeAuthorId = resolveRepresentativeAuthorId(duplicate, contributingByAuthorId, contributingByNumber);
            if ( representativeAuthorId == null ) {
                unmatchedDuplicates.add(duplicate);
                continue;
            }
            duplicate.put(DUPLICATE_OF, representativeAuthorId);
            duplicateByRepId.computeIfAbsent(representativeAuthorId, k -> new ArrayList<>()).add(duplicate);
        }
    }

    private static String resolveRepresentativeAuthorId(ObjectNode duplicate, TreeMap<String, ObjectNode> contributingByAuthorId,
            TreeMap<Integer, ObjectNode> contributingByNumber)
    {
        var duplicateOf = duplicate.path(DUPLICATE_OF).asText("").trim();
        if ( !duplicateOf.isBlank() && contributingByAuthorId.containsKey(duplicateOf) ) {
            return duplicateOf;
        }

        var contributingAuthorNumber = parsePositiveInt(duplicate.path(CONTRIBUTING_AUTHOR_NUMBER).asText(""));
        if ( contributingAuthorNumber <= 0 ) {
            return null;
        }

        var representative = contributingByNumber.get(contributingAuthorNumber);
        if ( representative == null ) {
            return null;
        }
        var authorId = representative.path(AUTHOR_ID).asText("").trim();
        return authorId.isBlank() ? null : authorId;
    }

    private static int parsePositiveInt(String value) {
        try {
            var parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : -1;
        } catch ( NumberFormatException e ) {
            return -1;
        }
    }

    private NcdReportContributorsCsvSchema() {}
}
