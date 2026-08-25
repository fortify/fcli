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

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Internal change detail captured during preview processing.
 * Includes line numbers, code snippets, and context metadata.
 * This is an internal representation that gets converted to FileChange for output.
 * 
 * @param changeIndex 1-based index of this change
 * @param lineFrom Starting line number (1-based)
 * @param lineTo Ending line number (1-based)
 * @param originalCode Code being replaced
 * @param newCode Replacement code
 * @param contextLinesBefore Number of context lines before the change
 * @param contextLinesAfter Number of context lines after the change
 * @param contextContent Full context text from remediations.xml
 * @param fuzzyMatched Whether fuzzy matching was used
 */
@Reflectable
public record ChangeDetail(
        int changeIndex,
        int lineFrom,
        int lineTo,
        String originalCode,
        String newCode,
        int contextLinesBefore,
        int contextLinesAfter,
        String contextContent,
        boolean fuzzyMatched) {

    public FileChange toFileChange() {
        ContextMetadata context = new ContextMetadata(contextLinesBefore, contextLinesAfter, contextContent);
        return FileChange.builder()
                .changeIndex(changeIndex)
                .lineFrom(lineFrom)
                .lineTo(lineTo)
                .originalCode(originalCode)
                .newCode(newCode)
                .context(context)
                .fuzzyMatched(fuzzyMatched)
                .build();
    }
}
