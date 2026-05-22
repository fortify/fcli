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
 * Manifest stored as {@code .fortify-extensions.json} in each target directory.
 * Tracks what was installed in that directory (files, version, content type)
 * without recording which assistants use this directory. This allows
 * recovery of installation state even if the fcli state is reset.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@Reflectable @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder = true) @Data
public class AiAssistExtensionsTargetDirManifest {
    public static final String MANIFEST_FILENAME = ".fortify-extensions.json";

    @JsonProperty("schema-version")
    private int schemaVersion;
    @JsonProperty("content-type")
    private String contentType;
    private String version;
    private String timestamp;
    private List<String> files;
}
