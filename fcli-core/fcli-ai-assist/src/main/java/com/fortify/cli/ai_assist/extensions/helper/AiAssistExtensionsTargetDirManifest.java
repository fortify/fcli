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
package com.fortify.cli.ai_assist.extensions.helper;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Manifest stored as {@code .fortify-extensions.<contentType>.json} in each
 * target directory. Tracks what was installed in that directory (files, version,
 * content type) without recording which assistants use this directory. This
 * allows recovery of installation state even if the fcli state is reset.
 * <p>
 * Each content type gets its own manifest file, so multiple content types can
 * coexist in the same target directory without overwriting each other.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@Reflectable @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder = true) @Data
public class AiAssistExtensionsTargetDirManifest {
    private static final String MANIFEST_PREFIX = ".fortify-extensions.";
    private static final String MANIFEST_SUFFIX = ".json";
    private static final String MANIFEST_GLOB = MANIFEST_PREFIX + "*" + MANIFEST_SUFFIX;

    public static String manifestFilename(String contentType) {
        return MANIFEST_PREFIX + sanitize(contentType) + MANIFEST_SUFFIX;
    }

    public static String manifestGlob() {
        return MANIFEST_GLOB;
    }

    static String sanitize(String contentType) {
        return contentType.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    @JsonProperty("schema-version")
    private int schemaVersion;
    @JsonProperty("content-type")
    private String contentType;
    private String version;
    private String timestamp;
    private List<String> files;
}
