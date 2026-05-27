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
 * Descriptor for content-manifest.yaml (embedded in the extensions zip).
 * Describes what content the archive contains and how to discover entries.
 */
@Reflectable @NoArgsConstructor @Data
public class AiAssistExtensionsContentManifestDescriptor {
    private int schemaVersion;
    @JsonProperty("content-types")
    private Map<String, AiAssistExtensionsContentTypeDescriptor> contentTypes;
}
