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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.aviator._common.exception.AviatorBugException;

import lombok.Builder;

@Reflectable
@Builder
@JsonPropertyOrder({"changeIndex", "lineFrom", "lineTo", "originalCode", "newCode", "context"})
public record PreviewFileChange(
        int changeIndex,
        int lineFrom,
        int lineTo,
        String originalCode,
        String newCode,
        ContextMetadata context) {
    public PreviewFileChange {
        if (changeIndex < 1) {
            throw new AviatorBugException("PreviewFileChange changeIndex must be positive");
        }
        if (lineFrom < 1 || lineTo < lineFrom) {
            throw new AviatorBugException("PreviewFileChange invalid line range: " + lineFrom + "-" + lineTo);
        }
        if (context == null) {
            throw new AviatorBugException("PreviewFileChange context is required");
        }
    }
}