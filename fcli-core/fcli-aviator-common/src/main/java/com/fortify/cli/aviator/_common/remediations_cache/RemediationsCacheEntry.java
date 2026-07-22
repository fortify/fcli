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
package com.fortify.cli.aviator._common.remediations_cache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Reflectable
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemediationsCacheEntry {
    private int order;
    private String artifactId;
    private String releaseId;
    private String uploadDate;
    private String path;
    private String sha256;

    /**
     * Factory for writers (not Jackson). Avoids multi-arg same-type call sites on the wire model.
     * Do not add {@code @Builder} on this {@code @Reflectable} class.
     */
    public static RemediationsCacheEntry of(
            int order, String path, String artifactId, String releaseId, String uploadDate, String sha256) {
        RemediationsCacheEntry entry = new RemediationsCacheEntry();
        entry.setOrder(order);
        entry.setPath(path);
        entry.setArtifactId(artifactId);
        entry.setReleaseId(releaseId);
        entry.setUploadDate(uploadDate);
        entry.setSha256(sha256);
        return entry;
    }
}
