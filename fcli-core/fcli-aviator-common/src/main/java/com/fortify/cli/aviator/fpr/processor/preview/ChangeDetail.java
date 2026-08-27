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

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Internal change detail captured during preview processing.
 * This is an internal representation that gets converted to FileChange for output.
 */
@Reflectable
@Builder
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ChangeDetail {
    private final int changeIndex;
    private final int lineFrom;
    private final int lineTo;
    private final String originalCode;
    private final String newCode;
    private final int contextLinesBefore;
    private final int contextLinesAfter;
    private final String contextContent;
    private final boolean fuzzyMatched;

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
