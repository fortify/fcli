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

import lombok.Builder;

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
    private final int declaredLineFrom;
    private final int declaredLineTo;
    private final int deltaLines;
    private final String comparisonCode;
    private final String[] lineNormalizedContent;

    public AppliedChange(int originalLineFrom, int originalLineTo, int deltaLines, String comparisonCode) {
        this(originalLineFrom, originalLineTo, originalLineFrom, originalLineTo, deltaLines, comparisonCode, null);
    }

    @Builder
    public AppliedChange(int originalLineFrom, int originalLineTo, int declaredLineFrom, int declaredLineTo, int deltaLines, String comparisonCode, String lineNormalizedCode) {
        this.originalLineFrom = originalLineFrom;
        this.originalLineTo = originalLineTo;
        this.declaredLineFrom = declaredLineFrom;
        this.declaredLineTo = declaredLineTo;
        this.deltaLines = deltaLines;
        this.comparisonCode = comparisonCode;
        // Store line-by-line normalized content for offset-anchored comparison (newlines preserved, each line normalized)
        if (lineNormalizedCode != null && !lineNormalizedCode.isEmpty()) {
            this.lineNormalizedContent = lineNormalizedCode.split("\n", -1);
        } else {
            this.lineNormalizedContent = null;
        }
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
        boolean overlaps = lineFrom <= originalLineTo && originalLineFrom <= lineTo;
        boolean candidateContainsThis = lineFrom <= originalLineFrom && originalLineTo <= lineTo;
        return overlaps && !coversFully(lineFrom, lineTo) && !candidateContainsThis;
    }

    /** True if this change's declared range fully contains [lineFrom, lineTo]. */
    public boolean coversDeclaredRange(int lineFrom, int lineTo) {
        return declaredLineFrom <= lineFrom && lineTo <= declaredLineTo;
    }

    /** True if [lineFrom, lineTo] overlaps this change's declared range without either side fully containing the other. */
    public boolean overlapsDeclaredRangePartially(int lineFrom, int lineTo) {
        boolean overlaps = lineFrom <= declaredLineTo && declaredLineFrom <= lineTo;
        boolean candidateContainsThis = lineFrom <= declaredLineFrom && declaredLineTo <= lineTo;
        return overlaps && !coversDeclaredRange(lineFrom, lineTo) && !candidateContainsThis;
    }

    /**
     * Below this length a normalized comparison code (e.g. {@code "return;"}) is too short and
     * generic to prove that a substring hit inside a broader fix's content is the same fix,
     * rather than an incidental match.
     */
    private static final int MIN_PROVEN_COVERAGE_LENGTH = 8;

    /**
     * True if this change's comparison code contains the candidate's comparison code at the
     * expected offset (offset-anchored matching). Calculates where the candidate hunk is
     * located relative to this broader fix (as an offset of lines), then checks if the candidate
     * content appears at that offset within this fix's content. Falls back to false (POSSIBLY_REMEDIATED)
     * if the substring appears but not at the expected offset (incidental match, not proof of coverage).
     *
     * <p>Unavailable content (either side {@code null}), blank candidates, and too-short
     * candidates prove nothing, so coverage is NOT assumed — callers fall back to
     * {@code POSSIBLY_REMEDIATED} rather than {@code SUPERSEDED}.
     */
    public boolean contentCovers(String candidateComparisonCode, int candidateLineFrom, int candidateLineTo) {
        if (candidateComparisonCode == null || comparisonCode == null || lineNormalizedContent == null) {
            return false;
        }
        if (candidateComparisonCode.isBlank() || candidateComparisonCode.length() < MIN_PROVEN_COVERAGE_LENGTH) {
            return false;
        }

        // Offset-anchored comparison: check if candidate appears at the expected offset within this fix's content
        // Calculate the offset: where does candidateLineFrom sit relative to originalLineFrom?
        int offsetFromAppliedStart = candidateLineFrom - originalLineFrom;
        int candidateLineCount = candidateLineTo - candidateLineFrom + 1;

        // Check if candidate fits at expected offset within applied content
        if (offsetFromAppliedStart < 0 || offsetFromAppliedStart + candidateLineCount > lineNormalizedContent.length) {
            return false;
        }

        // Reconstruct what we expect to see at the candidate's offset
        StringBuilder expectedAtOffset = new StringBuilder();
        for (int i = offsetFromAppliedStart; i < offsetFromAppliedStart + candidateLineCount; i++) {
            if (i > offsetFromAppliedStart) {
                expectedAtOffset.append("\n");
            }
            expectedAtOffset.append(lineNormalizedContent[i]);
        }

        // Exact match at expected offset (not just substring anywhere)
        return expectedAtOffset.toString().equals(candidateComparisonCode);
    }
}
