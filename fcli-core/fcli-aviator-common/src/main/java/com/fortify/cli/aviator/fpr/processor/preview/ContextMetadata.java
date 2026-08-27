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

/**
 * Context metadata from the remediations XML, including before/after line counts
 * and the full context text. Used for fuzzy matching when file hashes don't match.
 * 
 * @param linesBefore Number of context lines before the changed code
 * @param linesAfter Number of context lines after the changed code
 * @param content Full context text as a single string (may contain newlines)
 */
@Reflectable
@JsonPropertyOrder({"linesBefore", "linesAfter", "content"})
public record ContextMetadata(
        int linesBefore,
        int linesAfter,
        String content) {

    public ContextMetadata {
        if (linesBefore < 0) {
            throw new AviatorBugException("ContextMetadata linesBefore must be non-negative");
        }
        if (linesAfter < 0) {
            throw new AviatorBugException("ContextMetadata linesAfter must be non-negative");
        }
        content = content == null ? "" : content;
    }
}
