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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.aviator._common.exception.AviatorBugException;

@Reflectable
@JsonPropertyOrder({"path", "encoding", "changes"})
public record FilePreview(String path, String encoding, List<PreviewFileChange> changes) {
    public FilePreview {
        if (path == null || path.isBlank()) {
            throw new AviatorBugException("FilePreview path is required");
        }
        changes = changes == null ? List.of() : Collections.unmodifiableList(List.copyOf(changes));
    }

    public int totalChanges() {
        return changes.size();
    }
}