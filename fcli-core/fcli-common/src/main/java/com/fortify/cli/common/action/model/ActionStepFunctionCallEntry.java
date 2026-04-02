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

import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Describes a single function invocation within a {@code function.call} step.
 * The map key is the function name; this entry holds the {@code var-name} for
 * storing the return value and all argument name/value pairs.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonClassDescription("Invoke a function defined in the functions: section.")
public final class ActionStepFunctionCallEntry extends AbstractActionStepElement implements IMapKeyAware<String> {
    @JsonIgnore @Getter private String key;

    @JsonPropertyDescription("Variable name in which to store the function's return value.")
    @JsonProperty(value = "var-name", required = true)
    private String varName;

    @JsonIgnore
    private LinkedHashMap<String, TemplateExpression> args = new LinkedHashMap<>();

    @JsonAnySetter
    public void setArg(String name, TemplateExpression value) {
        args.put(name, value);
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    public void postLoad(Action action) {
        Action.checkNotBlank("var-name", varName, this);
    }
}
