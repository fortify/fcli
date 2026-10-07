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

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wire model for one FPR in a remediations cache manifest.
 * Product identity is nested ({@link #sscData} <em>or</em> {@link #fodData}), not sibling
 * optional ids on this type. Prefer {@link #forSsc} / {@link #forFod} with a product model
 * from {@link SSCData#of} / {@link FoDData#of}; do not add {@code @Builder} on this
 * {@code @Reflectable} class.
 */
@Reflectable
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemediationsCacheEntry {
    private int order;
    private String path;
    private String sha256;
    private SSCData sscData;
    private FoDData fodData;

    /**
     * Factory for SSC cache writers (not Jackson). Assigns shared fields and the provided
     * SSC product block; product field construction belongs on {@link SSCData#of}.
     */
    public static RemediationsCacheEntry forSsc(int order, String path, String sha256, SSCData sscData) {
        FcliSimpleException.throwIf(sscData == null, "sscData is required for an SSC remediations cache entry");
        RemediationsCacheEntry entry = new RemediationsCacheEntry();
        entry.setOrder(order);
        entry.setPath(path);
        entry.setSha256(sha256);
        entry.setSscData(sscData);
        return entry;
    }

    /**
     * Factory for FoD cache writers (not Jackson). Assigns shared fields and the provided
     * FoD product block; product field construction belongs on {@link FoDData#of}.
     */
    public static RemediationsCacheEntry forFod(int order, String path, String sha256, FoDData fodData) {
        FcliSimpleException.throwIf(fodData == null, "fodData is required for a FoD remediations cache entry");
        RemediationsCacheEntry entry = new RemediationsCacheEntry();
        entry.setOrder(order);
        entry.setPath(path);
        entry.setSha256(sha256);
        entry.setFodData(fodData);
        return entry;
    }

    /** Validates structural invariants (no cache path context). */
    public void validate() {
        validate(null);
    }

    /**
     * Validates structural invariants of this entry (path, checksum, exactly one product block).
     * Product-vs-manifest consistency is checked by {@link RemediationsCacheManifest#validate(Path)}.
     *
     * @param cacheZip optional cache path included in error messages for diagnostics
     */
    public void validate(Path cacheZip) {
        String where = where(cacheZip);
        FcliSimpleException.throwIf(StringUtils.isBlank(path),
                "Remediations cache entry is missing path%s", where);
        FcliSimpleException.throwIf(StringUtils.isBlank(sha256),
                "Remediations cache entry is missing sha256: %s%s", path, where);
        boolean hasSsc = sscData != null;
        boolean hasFod = fodData != null;
        FcliSimpleException.throwIf(hasSsc == hasFod,
                "Remediations cache entry must have exactly one of sscData or fodData: %s%s", path, where);
        if (hasSsc) {
            sscData.validate(path, where);
        } else {
            fodData.validate(path, where);
        }
    }

    private static String where(Path cacheZip) {
        return cacheZip != null ? " (" + cacheZip + ")" : "";
    }

    /** SSC-specific fields for one cache entry. */
    @Reflectable
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SSCData {
        private String artifactId;
        private String uploadDate;

        /** Factory for writers (not Jackson). */
        public static SSCData of(String artifactId, String uploadDate) {
            SSCData ssc = new SSCData();
            ssc.setArtifactId(artifactId);
            ssc.setUploadDate(uploadDate);
            return ssc;
        }

        void validate(String entryPath, String where) {
            FcliSimpleException.throwIf(StringUtils.isBlank(artifactId),
                    "Remediations cache SSC entry is missing artifactId: %s%s", entryPath, where);
        }
    }

    /** FoD-specific fields for one cache entry. */
    @Reflectable
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FoDData {
        private String releaseId;

        /** Factory for writers (not Jackson). */
        public static FoDData of(String releaseId) {
            FoDData fod = new FoDData();
            fod.setReleaseId(releaseId);
            return fod;
        }

        void validate(String entryPath, String where) {
            FcliSimpleException.throwIf(StringUtils.isBlank(releaseId),
                    "Remediations cache FoD entry is missing releaseId: %s%s", entryPath, where);
        }
    }
}
