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
package com.fortify.cli.aviator.fpr.remediation;

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

     final String displayName;

    SkipReason(String displayName) {
        this.displayName = displayName;
    }

     final String displayName() {
        return displayName;
    }
}
