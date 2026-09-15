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
package com.fortify.cli.aviator.fpr.remediation.model;

/**
 * Offset-map entry: records a hunk that was actually written to a file this run,
 * in terms of the PRISTINE file's line numbers. {@code deltaLines} is
 * (newLineCount - originalLineCount); positive means the file grew, negative means it shrunk.
 *
 * <p>Was a record; promoted to a class because the line-range/content comparisons in
 * {@code classifyRange} are logic that reads these fields and belongs on the object that owns
 * them, not as free functions in the caller.
 */
public final class AppliedChange {
    private final int originalLineFrom;
    private final int originalLineTo;
    private final int deltaLines;
    private final String comparisonCode;

    public AppliedChange(int originalLineFrom, int originalLineTo, int deltaLines, String comparisonCode) {
        this.originalLineFrom = originalLineFrom;
        this.originalLineTo = originalLineTo;
        this.deltaLines = deltaLines;
        this.comparisonCode = comparisonCode;
    }

    public int originalLineTo() {
        return originalLineTo;
    }

    public int deltaLines() {
        return deltaLines;
    }

    /** True if this change's original range fully contains [lineFrom, lineTo]. */
    public boolean coversFully(int lineFrom, int lineTo) {
        return originalLineFrom <= lineFrom && lineTo <= originalLineTo;
    }

    /** True if [lineFrom, lineTo] overlaps this change's original range without either side fully containing the other. */
    public boolean overlapsPartially(int lineFrom, int lineTo) {
        boolean disjoint = lineTo < originalLineFrom || lineFrom > originalLineTo;
        return !disjoint;
    }

    /**
     * Below this length a normalized comparison code (e.g. {@code "return;"}) is too short and
     * generic to prove that a substring hit inside a broader fix's content is the same fix,
     * rather than an incidental match.
     */
    private static final int MIN_PROVEN_COVERAGE_LENGTH = 8;

    /**
     * True if this change's own comparison code contains the candidate's comparison code
     * (normalized substring match). Unavailable content (either side {@code null}) and
     * too-short/blank candidates prove nothing, so coverage is NOT assumed in those cases —
     * callers fall back to {@code POSSIBLY_REMEDIATED} rather than {@code SUPERSEDED}.
     */
    public boolean contentCovers(String candidateComparisonCode) {
        if (candidateComparisonCode == null || comparisonCode == null) {
            return false;
        }
        if (candidateComparisonCode.length() < MIN_PROVEN_COVERAGE_LENGTH) {
            return false;
        }
        return comparisonCode.contains(candidateComparisonCode);
    }
}
