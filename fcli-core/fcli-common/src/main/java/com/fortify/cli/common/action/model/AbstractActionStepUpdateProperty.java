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
 * This abstract class describes an operation to update a data property.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public abstract class AbstractActionStepUpdateProperty extends AbstractActionStep implements IActionStepValueSupplier {
    @JsonPropertyDescription("Required string: Name to assign to the outcome of this operation. Can be referenced in subsequent steps using ${[name]}.")
    @JsonProperty(required = true) private String name;
    
    @JsonPropertyDescription("Required SpEL template expression if 'valueTemplate' is not specified: Value to be assigned or appended to the given name.")
    @JsonProperty(required = false) private TemplateExpression value;
    
    @JsonPropertyDescription("Required string if 'value' is not specified: Name of a value template to be evaluated, assigning or appending the outcome of the value template to the given set/append name.")
    @JsonProperty(required = false) private String valueTemplate;
    
    public final void postLoad(Action action) {
        Action.checkNotBlank("set name", name, this);
        Action.checkActionValueSupplier(action, this);
        _postLoad(action);
    }
    
    protected void _postLoad(Action action) {}
}