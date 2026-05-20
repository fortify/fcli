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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-file state descriptor stored under the fcli state directory.
 * Tracks what was installed, where, and from which source version.
 */
@Reflectable @NoArgsConstructor @AllArgsConstructor @Builder @Data
public class AgentExtensionsStateDescriptor {
    private String assistant;
    private String assistantId;
    private String file;
    private String contentType;
    private String targetDir;
    private String targetPath;
    private String sourceVersion;
    @JsonProperty("timestamp")
    private String timestamp;
}
