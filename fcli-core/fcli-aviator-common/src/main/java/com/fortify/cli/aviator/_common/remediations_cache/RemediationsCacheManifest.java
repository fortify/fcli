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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;

import lombok.Data;
import lombok.NoArgsConstructor;

@Reflectable
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemediationsCacheManifest {
    private int schemaVersion;
    private String kind;
    private String product;
    private String createdAt;
    private Map<String, String> selection = new LinkedHashMap<>();
    private List<RemediationsCacheEntry> entries = new ArrayList<>();

    /** Validates schema, product, and every entry (no cache path context). */
    public void validate() {
        validate(null);
    }

    /**
     * Validates schema, product, and every entry (including product-block consistency).
     * Call after Jackson load and before publishing a written cache.
     *
     * @param cacheZip optional cache path included in error messages for diagnostics
     */
    public void validate(Path cacheZip) {
        String where = cacheZip != null ? ": " + cacheZip : "";
        FcliSimpleException.throwIf(schemaVersion != RemediationsCacheConstants.SCHEMA_VERSION,
                "Unsupported remediations cache schemaVersion %s (expected %s)%s",
                schemaVersion, RemediationsCacheConstants.SCHEMA_VERSION, where);
        FcliSimpleException.throwIf(!RemediationsCacheConstants.KIND.equals(kind),
                "Invalid remediations cache kind '%s' (expected %s)%s",
                kind, RemediationsCacheConstants.KIND, where);
        FcliSimpleException.throwIf(StringUtils.isBlank(product),
                "Remediations cache manifest is missing product%s", where);
        boolean sscProduct = RemediationsCacheConstants.PRODUCT_SSC.equals(product);
        boolean fodProduct = RemediationsCacheConstants.PRODUCT_FOD.equals(product);
        FcliSimpleException.throwIf(!sscProduct && !fodProduct,
                "Unsupported remediations cache product '%s' (expected %s or %s)%s",
                product, RemediationsCacheConstants.PRODUCT_SSC, RemediationsCacheConstants.PRODUCT_FOD, where);
        FcliSimpleException.throwIf(entries == null || entries.isEmpty(),
                "Remediations cache has no entries%s", where);
        for (RemediationsCacheEntry entry : entries) {
            FcliSimpleException.throwIf(entry == null,
                    "Remediations cache contains a null entry%s", where);
            entry.validate(cacheZip);
            if (sscProduct) {
                FcliSimpleException.throwIf(entry.getSscData() == null,
                        "SSC remediations cache entry is missing sscData: %s%s",
                        entry.getPath(), where);
            } else {
                FcliSimpleException.throwIf(entry.getFodData() == null,
                        "FoD remediations cache entry is missing fodData: %s%s",
                        entry.getPath(), where);
            }
        }
    }
}
