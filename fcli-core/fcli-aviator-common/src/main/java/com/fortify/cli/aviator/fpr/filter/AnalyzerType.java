/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.aviator.fpr.filter;

public enum AnalyzerType {
    DATAFLOW("dataflow", "Data Flow"),
    CONTROLFLOW("controlflow", "Control Flow"),
    STRUCTURAL("structural", "Structural"),
    CONFIGURATION("configuration", "Configuration"),
    SEMANTIC("semantic", "Semantic"),
    FINDBUGS("findbugs", "Findbugs"),
    CONTENT("content", "Content"),
    BUFFER("buffer", "Buffer"),
    TRACER_INTEGRATION("tracerintegration", "TracerIntegration"),
    NULLPTR("nullptr", "NullPtr"),
    PENTEST("pentest", "Pentest"),
    STATISTICAL("statistical", "Statistical"),
    REMOVED("removedissue", "RemovedIssue"),
    CUSTOM("customissue", "CustomIssue");

    private final String legacyName;
    private final String canonicalName;

    AnalyzerType(String legacyName, String canonicalName) {
        this.legacyName = legacyName;
        this.canonicalName = canonicalName;
    }

    public String getLegacyName() {
        return legacyName;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public static String convertAnalyzerForLegacyMappings(String analyzer) {
        for (AnalyzerType type : values()) {
            if (type.canonicalName.equals(analyzer)) {
                return type.legacyName;
            }
        }
        // Default case: uncapitalize if not found
        return uncapitalize(analyzer);
    }

    public static String canonicalizeAnalyzerName(String analyzerName) {
        for (AnalyzerType type : values()) {
            if (type.legacyName.equalsIgnoreCase(analyzerName)) {
                return type.canonicalName;
            }
        }
        // Default case: capitalize if not found
        return capitalize(analyzerName);
    }

    // Assuming these helper methods exist in your codebase
    private static String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
