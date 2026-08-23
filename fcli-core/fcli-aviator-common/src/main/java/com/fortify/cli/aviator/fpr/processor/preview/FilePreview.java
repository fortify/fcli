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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Preview information for a single file in a remediation.
 * Contains metadata about the file and all code changes that would be applied.
 * 
 * @param path The relative file path as stored in FVDL (e.g., "src/Example.java") - kept relative for security (does not expose absolute filesystem paths)
 * @param encoding The character encoding used to read/write the file (from FVDL metadata)
 * @param changes List of individual code changes within this file
 */
@Reflectable
@JsonPropertyOrder({"path", "encoding", "changes"})
public record FilePreview(
        String path,
        String encoding,
        List<FileChange> changes) {

    public FilePreview {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("FilePreview path is required");
        }
        changes = changes == null ? List.of() : Collections.unmodifiableList(List.copyOf(changes));
    }

    public int totalChanges() {
        return changes.size();
    }
}
