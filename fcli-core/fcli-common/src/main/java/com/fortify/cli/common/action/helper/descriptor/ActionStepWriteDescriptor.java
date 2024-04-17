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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'write' step.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionStepWriteDescriptor implements IActionStepIfSupplier, IActionStepValueSupplier {
    /** Optional if-expression, executing this step only if condition evaluates to true */
    @JsonProperty("if") private TemplateExpression _if;
    /** Required template expression defining where to write the data, either stdout, stderr or filename */
    private TemplateExpression to;
    /** Value template expression that generates the contents to be written */
    private TemplateExpression value;
    /** Value template for this step element */
    private String valueTemplate;
    
    
    public void postLoad(ActionDescriptor action) {
        ActionDescriptor.checkNotNull("write to", to, this);
        ActionDescriptor.checkActionValueSupplier(action, this);
    }
}