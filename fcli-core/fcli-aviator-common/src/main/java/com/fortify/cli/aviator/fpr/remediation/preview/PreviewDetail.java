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
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.aviator._common.exception.AviatorBugException;

@Reflectable
@JsonPropertyOrder({"issueId", "status", "description", "files"})
public record PreviewDetail(String issueId, String status, String description, Map<String, FilePreview> files) {
    public PreviewDetail {
        if (issueId == null || issueId.isBlank()) {
            throw new AviatorBugException("PreviewDetail issueId is required");
        }
        if (status == null || status.isBlank()) {
            throw new AviatorBugException("PreviewDetail status is required");
        }
        files = files == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(files));
    }

    public static PreviewDetail available(String issueId, String description, Map<String, FilePreview> files) {
        return new PreviewDetail(issueId, "available", description, files);
    }

    public static PreviewDetail skipped(String issueId, String description) {
        return new PreviewDetail(issueId, "skipped", description, Map.of());
    }

    @JsonIgnore
    public boolean isAvailable() {
        return "available".equals(status);
    }

    @JsonIgnore
    public boolean isSkipped() {
        return "skipped".equals(status);
    }
}