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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This abstract class is the base class for forEach steps/properties.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public abstract class AbstractActionStepForEach extends AbstractActionStep {
    /** Optional break-expression, terminating forEach if condition evaluates to true */
    private TemplateExpression breakIf;
    /** Required name for this step element */
    private String name;
    /** Steps to be repeated for each value */
    @JsonProperty("do") private List<ActionStep> _do;
    
    /**
     * This method is invoked by the {@link ActionStep#postLoad()}
     * method. It checks that required properties are set, then calls the postLoad() method for
     * each sub-step.
     */
    public final void postLoad(Action action) {
        Action.checkNotBlank("forEach name", name, this);
        Action.checkNotNull("forEach do", _do, this);
        _postLoad(action);
    }

    protected void _postLoad(Action action) {}
}