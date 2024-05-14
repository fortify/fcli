/**
 * Copyright 2023 Open Text.
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'write' step.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public final class ActionStepWrite extends AbstractActionStep implements IActionStepValueSupplier {
    @JsonPropertyDescription("Required SpEL template expression: Specify where to write the given data; either 'stdout', 'stderr' or a filename.")
    @JsonProperty(required = true) private TemplateExpression to;
    
    @JsonPropertyDescription("Required SpEL template expression if 'valueTemplate' is not specified: Value to be written to the given output.")
    @JsonProperty(required = false) private TemplateExpression value;
    
    @JsonPropertyDescription("Required string if 'value' is not specified: Name of a value template to be evaluated, writing the outcome of the value template to the given output.")
    @JsonProperty(required = false) private String valueTemplate;    
    
    public void postLoad(Action action) {
        Action.checkNotNull("write to", to, this);
        Action.checkActionValueSupplier(action, this);
    }
}