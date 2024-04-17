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
package com.fortify.cli.common.action.helper.descriptor;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This abstract class is the base class for forEach steps/properties.
 */
@Reflectable @NoArgsConstructor
@Data
public abstract class AbstractActionStepForEachDescriptor implements IActionStepIfSupplier {
    /** Optional if-expression, executing steps only if condition evaluates to true */
    @JsonProperty("if") private TemplateExpression _if;
    /** Optional break-expression, terminating forEach if condition evaluates to true */
    private TemplateExpression breakIf;
    /** Required name for this step element */
    private String name;
    /** Steps to be repeated for each value */
    @JsonProperty("do") private List<ActionStepDescriptor> _do;
    
    /**
     * This method is invoked by the {@link ActionStepDescriptor#postLoad()}
     * method. It checks that required properties are set, then calls the postLoad() method for
     * each sub-step.
     */
    public final void postLoad(ActionDescriptor action) {
        ActionDescriptor.checkNotBlank("forEach name", name, this);
        ActionDescriptor.checkNotNull("forEach do", _do, this);
        _do.forEach(d->d.postLoad(action));
        _postLoad(action);
    }

    protected void _postLoad(ActionDescriptor action) {}
}