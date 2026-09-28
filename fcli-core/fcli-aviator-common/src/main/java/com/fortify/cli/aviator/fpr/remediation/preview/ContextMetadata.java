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

@Reflectable
@JsonPropertyOrder({"linesBefore", "linesAfter", "content"})
public record ContextMetadata(int linesBefore, int linesAfter, String content) {
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