/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.action.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;
import com.fortify.cli.common.log.LogSensitivityLevel;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes an action option mask.
 */
@Reflectable @NoArgsConstructor
@Data
@JsonTypeName("mask")
@JsonClassDescription("Define log masking settings.")
@SampleYamlSnippets({"""
        config:
        mask.env-vars:
            SOME_PASSWORD:
            sensitivity: high
        ""","""
        cli.options:
        pwd:
            names: -p,--password
            mask: {sensitivity: high}
        """})
public final class ActionInputMask implements IActionElement {
    @JsonPropertyDescription("""
        Optional enum value: Value sensitivity; high/medium/low. Default value: high   
        """)
    @JsonProperty(value = "sensitivity", required = false) private LogSensitivityLevel sensitivityLevel = LogSensitivityLevel.high;
    
    @JsonPropertyDescription("""
        Optional string: Mask description, used to generate the masked output.
        """)
    @JsonProperty(value = "description", required = false) private String description;
    
    @JsonPropertyDescription("""
        Optional string: Pattern for fine-tuning which value contents should be
        masked. Every matching regex group will be masked using the given description.
        If no pattern is defined, or if the pattern doesn't contain any groups, the
        full value will be masked.
        """)
    @JsonProperty(value = "pattern", required = false) private String pattern;
    
    public final void postLoad(Action action) {
    }
}