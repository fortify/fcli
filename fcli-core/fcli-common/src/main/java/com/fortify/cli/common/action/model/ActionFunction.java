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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Describes a reusable function defined in an action YAML's {@code functions:} section.
 */
@Reflectable @NoArgsConstructor
@Data
@JsonInclude(Include.NON_NULL)
@JsonClassDescription("Define a reusable function that can be called from action steps.")
public final class ActionFunction implements IActionElement, IMapKeyAware<String> {
    @JsonIgnore @Getter private String key;

    @JsonPropertyDescription("Human-readable description of this function.")
    @JsonProperty(value = "description", required = false)
    private String description;

    @JsonPropertyDescription("Function arguments, keyed by argument name. Preserves declaration order for positional invocation.")
    @JsonProperty(value = "args", required = false)
    private LinkedHashMap<String, ActionFunctionArg> args;

    @JsonPropertyDescription("SpEL template expression for the return value. Defaults to ${_result} if omitted.")
    @JsonProperty(value = "return", required = false)
    private TemplateExpression _return;

    @JsonPropertyDescription("Steps to execute when the function is called.")
    @JsonProperty(value = "steps", required = true)
    private List<ActionStep> steps;

    @JsonPropertyDescription("Whether this function uses streaming (fn.yield). Auto-detected if omitted.")
    @JsonProperty(value = "streaming", required = false)
    private Boolean streaming;

    @JsonPropertyDescription("Whether this function is exported for external use (RPC/MCP). Defaults to true.")
    @JsonProperty(value = "export", required = false)
    private Boolean export;

    @JsonPropertyDescription("Generic metadata map for server exposure configuration (e.g., mcp.resource, mcp.prompt).")
    @JsonProperty(value = "meta", required = false)
    private ObjectNode meta;

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, ActionFunctionArg> getArgsOrEmpty() {
        return args != null ? args : Collections.emptyMap();
    }

    public boolean isExported() {
        return export == null || export;
    }

    public boolean isStreaming() {
        return streaming != null && streaming;
    }

    @Override
    public void postLoad(Action action) {
        Action.checkNotNull("steps", steps, this);
        if (streaming == null) {
            streaming = containsFnYield(steps);
        }
        if (Boolean.FALSE.equals(streaming) && containsFnYield(steps)) {
            throw new FcliActionValidationException(
                    "Function '" + key + "' has streaming=false but contains fn.yield steps");
        }
    }

    private static boolean containsFnYield(List<ActionStep> steps) {
        if (steps == null) { return false; }
        for (var step : steps) {
            if (step.getFnYield() != null) { return true; }
            if (step.getSteps() != null && containsFnYield(step.getSteps())) { return true; }
        }
        return false;
    }
}
