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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-assistant configuration from extensions-distribution.yaml.
 */
@Reflectable @NoArgsConstructor @Data
public class AiAssistExtensionsAssistantDescriptor {
    @JsonProperty("display-name")
    private String displayName;
    // Typed as Object to support recursive condition structures (maps for
    // operators like any-of/all-of/not, strings for leaf values). This
    // allows instanceof-based dispatch in AiAssistExtensionsConditionEvaluator
    // and graceful warning on unknown condition types instead of parse failures.
    @JsonProperty("if")
    private Object ifCondition;
    private List<AiAssistExtensionsTargetDescriptor> targets;
}
