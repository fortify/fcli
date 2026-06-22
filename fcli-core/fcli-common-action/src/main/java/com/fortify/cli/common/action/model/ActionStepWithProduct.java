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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'with.product' step that establishes product-specific
 * context (SpEL functions + REST targets) for the steps in the {@code do} block.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonTypeName("withProduct")
@JsonClassDescription("Run the steps in the `do` block within the context of a product session (e.g., SSC or FoD).")
public final class ActionStepWithProduct extends AbstractActionStepElement {
    @JsonPropertyDescription("Product name (e.g., 'ssc', 'fod').")
    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonPropertyDescription("Session name to use. Defaults to 'default' if not specified.")
    @JsonProperty(value = "session", required = false)
    private TemplateExpression session;

    @JsonPropertyDescription("Steps to execute within the product context.")
    @JsonProperty(value = "do", required = true)
    private List<ActionStep> _do;

    @Override
    public void postLoad(Action action) {
        Action.checkNotBlank("name", name, this);
        Action.checkNotNull("do", _do, this);
    }
}
