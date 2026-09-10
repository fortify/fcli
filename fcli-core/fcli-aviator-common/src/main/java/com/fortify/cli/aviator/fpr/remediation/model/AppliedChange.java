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
    private final String instanceId;
    private final int originalLineFrom;
    private final int originalLineTo;
    private final int deltaLines;
    private final String comparisonCode;

    public AppliedChange(String instanceId, int originalLineFrom, int originalLineTo, int deltaLines, String comparisonCode) {
        this.instanceId = instanceId;
        this.originalLineFrom = originalLineFrom;
        this.originalLineTo = originalLineTo;
        this.deltaLines = deltaLines;
        this.comparisonCode = comparisonCode;
    }

    public String instanceId() {
        return instanceId;
    }

    public int originalLineFrom() {
        return originalLineFrom;
    }

    public int originalLineTo() {
        return originalLineTo;
    }

    public int deltaLines() {
        return deltaLines;
    }

    public String comparisonCode() {
        return comparisonCode;
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
     * True if this change's own comparison code contains the candidate's comparison code
     * (normalized substring match), or if either side is unavailable for comparison — in which
     * case the caller conservatively treats coverage as proven.
     */
    public boolean contentCovers(String candidateComparisonCode) {
        return candidateComparisonCode == null || comparisonCode == null || comparisonCode.contains(candidateComparisonCode);
    }
}
