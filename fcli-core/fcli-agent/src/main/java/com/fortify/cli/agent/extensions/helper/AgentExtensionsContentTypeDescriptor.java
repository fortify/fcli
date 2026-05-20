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
package com.fortify.cli.agent.extensions.helper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Content type configuration from extensions-distribution.yaml.
 * Defines how entries are discovered within the source archive.
 */
@Reflectable @NoArgsConstructor @Data
public class AgentExtensionsContentTypeDescriptor {
    @JsonProperty("source-dir")
    private String sourceDir;
    private String discover;
    @JsonProperty("entry-marker")
    private String entryMarker;
    @JsonProperty("file-pattern")
    private String filePattern;
}
