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
package com.fortify.cli.common.action.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Describes a single argument of an action function.
 */
@Reflectable @NoArgsConstructor
@Data
@JsonInclude(Include.NON_NULL)
@JsonClassDescription("Define an argument for an action function.")
public final class ActionFunctionArg implements IActionElement {
    @JsonPropertyDescription("Argument type: string (default), boolean, int, long, double, float, array, or object.")
    @JsonProperty(value = "type", required = false)
    private String type;

    @JsonPropertyDescription("Whether this argument is required.")
    @JsonProperty(value = "required", required = false)
    private Boolean required;

    @JsonPropertyDescription("Default value expression, used when the argument is not provided.")
    @JsonProperty(value = "default", required = false)
    private TemplateExpression defaultValue;

    @JsonPropertyDescription("Description of this argument.")
    @JsonProperty(value = "description", required = false)
    private String description;

    @Override
    public void postLoad(Action action) {
        // No validation needed — all fields are optional
    }
}
