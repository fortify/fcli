package com.fortify.cli.aviator.fpr.remediation.model;

public enum SkipReason {
    SOURCE_FILE_MISSING("Source file missing"),
    SOURCE_FILE_OUTSIDE_SOURCE_DIR("Source file outside source directory"),
    SOURCE_READ_FAILED("Source file read failed"),
    SOURCE_DECODE_FAILED("Source file decode failed"),
    REMEDIATION_DATA_INVALID("Remediation data invalid"),
    REMEDIATION_LINE_RANGE_INVALID("Remediation line range invalid"),
    SOURCE_CONTEXT_NOT_FOUND("Source context not found"),
    SOURCE_CONTEXT_AMBIGUOUS("Source context matched multiple locations"),
    ORIGINAL_CODE_NOT_FOUND("Original code not found"),
    ORIGINAL_CODE_AMBIGUOUS("Original code matched multiple locations"),
    SUPERSEDED_BY_BROADER_FIX("Superseded by broader fix"),
    CONFLICTS_WITH_ANOTHER_FIX("Conflicts with another fix"),
    ANCHOR_DOES_NOT_MATCH("Anchor does not match"),
    REMEDIATION_ENCODE_FAILED("Remediation encode failed"),
    SOURCE_WRITE_FAILED("Source file write failed"),
    NO_CHANGES("No file changes found"),
    UNEXPECTED_ERROR("Unexpected remediation processing error");

    public final String displayName;

    SkipReason(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
