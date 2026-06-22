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

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Distribution descriptor for agent extensions (from tool-definitions zip).
 * Contains only the assistant mapping (detection + target directories).
 * Content type definitions come from the content-manifest.yaml in the extensions zip.
 */
@Reflectable @NoArgsConstructor @Data
public class AiAssistExtensionsDistributionDescriptor {
    private int schemaVersion;
    private Map<String, AiAssistExtensionsAssistantDescriptor> assistants;
}
