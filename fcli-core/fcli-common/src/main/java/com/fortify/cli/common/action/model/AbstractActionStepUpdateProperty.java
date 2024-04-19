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
    /** Required name for this step element */
    private String name;
    /** Value template expression for this step element */
    private TemplateExpression value;
    /** Value template for this step element */
    private String valueTemplate;
    
    public final void postLoad(Action action) {
        Action.checkNotBlank("set name", name, this);
        Action.checkActionValueSupplier(action, this);
        _postLoad(action);
    }
    
    protected void _postLoad(Action action) {}
}