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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Content type configuration from content-manifest.yaml.
 * Defines how entries are discovered within the source archive.
 */
@Reflectable @NoArgsConstructor @Data
public class AiAssistExtensionsContentTypeDescriptor {
    @JsonProperty("source-dir")
    private String sourceDir;
    private String discover;
    @JsonProperty("entry-marker")
    private String entryMarker;
    @JsonProperty("file-pattern")
    private String filePattern;
    /** Named entries for explicit discovery mode. Key = logical name, value = path in archive. */
    private Map<String, String> entries;
}
