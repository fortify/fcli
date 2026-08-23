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
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Preview details for a single remediation (issue ID), containing all file changes.
 * This record is serialized to JSON for IDE plugin consumption.
 * 
 * @param issueId The issue/remediation ID from the remediations.xml file
 * @param status Either "available" (successfully processed) or "skipped" (processing failed)
 * @param files Map of filename to FilePreview objects containing change details
 * @param skipReason Human-readable reason why remediation was skipped (null if status is "available")
 */
@Reflectable
@JsonPropertyOrder({"issueId", "status", "files", "available", "skipped", "skipReason"})
public record PreviewDetail(
        String issueId,
        String status,
        Map<String, FilePreview> files,
        String skipReason) {

    public PreviewDetail {
        if (issueId == null || issueId.isBlank()) {
            throw new IllegalArgumentException("PreviewDetail issueId is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("PreviewDetail status is required");
        }
        files = files == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(files));
    }

    public static PreviewDetail available(String issueId, Map<String, FilePreview> files) {
        return new PreviewDetail(issueId, "available", files, null);
    }

    public static PreviewDetail skipped(String issueId, String skipReason) {
        return new PreviewDetail(issueId, "skipped", Map.of(), skipReason);
    }

    public boolean isAvailable() {
        return "available".equals(status);
    }

    public boolean isSkipped() {
        return "skipped".equals(status);
    }
}
