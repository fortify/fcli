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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Output record produced by install/update/uninstall/list-installed commands.
 * One row per (assistant, contentType, targetDir) grouping.
 */
@Reflectable @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder = true) @Data
public class AiAssistExtensionsOutputDescriptor {
    private String assistant;
    private String assistantId;
    private String contentType;
    private String targetDir;
    private int fileCount;
    private String sourceVersion;
    /** Array of individual file paths (relative to targetDir). */
    private String[] files;
    /** Concatenated file paths for display. */
    private String filesString;
    @JsonProperty(IActionCommandResultSupplier.actionFieldName)
    private String actionResult;
}
