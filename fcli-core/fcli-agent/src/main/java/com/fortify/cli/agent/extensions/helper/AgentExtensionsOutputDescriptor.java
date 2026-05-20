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
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Output record produced by install/update/uninstall/status commands.
 * When serialized, the __action__ field is included in the JSON output.
 */
@Reflectable @NoArgsConstructor @AllArgsConstructor @Builder @Data
public class AgentExtensionsOutputDescriptor {
    private String assistant;
    private String assistantId;
    private String file;
    private String contentType;
    private String targetDir;
    private String targetPath;
    private String sourceVersion;
    @JsonProperty(IActionCommandResultSupplier.actionFieldName)
    private String actionResult;
}
