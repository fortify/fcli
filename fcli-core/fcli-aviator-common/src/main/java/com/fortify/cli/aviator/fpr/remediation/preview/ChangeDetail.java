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
package com.fortify.cli.aviator.fpr.remediation.preview;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

    public PreviewFileChange toPreviewFileChange() {
        ContextMetadata context = new ContextMetadata(contextLinesBefore, contextLinesAfter, contextContent);
        return PreviewFileChange.builder()
                .changeIndex(changeIndex)
                .lineFrom(lineFrom)
                .lineTo(lineTo)
                .originalCode(originalCode)
                .newCode(newCode)
                .context(context)
                .build();
    }
}