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
package com.fortify.cli.aviator.fpr.processor.preview;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.aviator._common.exception.AviatorBugException;

import lombok.Builder;

/**
 * A single code change within a file remediation, with context metadata.
 * Represents one transformation: replacing lines lineFrom-lineTo with newCode.
 * 
 * @param changeIndex 1-based index of this change within the file (for ordering)
 * @param lineFrom Starting line number (1-based, inclusive)
 * @param lineTo Ending line number (1-based, inclusive)
 * @param originalCode The code being replaced
 * @param newCode The replacement code
 * @param context Context lines surrounding the change (for validation)
 * @param fuzzyMatched True if file hash didn't match and fuzzy context search was used
 */
@Reflectable
@Builder
@JsonPropertyOrder({"changeIndex", "lineFrom", "lineTo", "originalCode", "newCode", "context", "fuzzyMatched"})
public record FileChange(
        int changeIndex,
        int lineFrom,
        int lineTo,
        String originalCode,
        String newCode,
        ContextMetadata context,
        boolean fuzzyMatched) {

    public FileChange {
        if (changeIndex < 1) {
            throw new AviatorBugException("FileChange changeIndex must be positive");
        }
        if (lineFrom < 1 || lineTo < lineFrom) {
            throw new AviatorBugException("FileChange invalid line range: " + lineFrom + "-" + lineTo);
        }
        if (context == null) {
            throw new AviatorBugException("FileChange context is required");
        }
    }
}
