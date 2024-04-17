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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a forEach element, allowing iteration over the output of
 * a given input.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionStepFcliDescriptor implements IActionStepIfSupplier {
    /** Optional if-expression, executing steps only if condition evaluates to true */
    @JsonProperty("if") private TemplateExpression _if;
    /** Required template expression providing fcli command to run */
    private TemplateExpression cmd;
    /** Optional name for this step element */
    private String name;
    /** Steps to be repeated for each value */
    @JsonProperty("forEach") private ActionStepFcliDescriptor.ActionStepFcliForEachDescriptor forEach;
    
    /**
     * This method is invoked by the {@link ActionStepDescriptor#postLoad()}
     * method. It checks that required properties are set, then calls the postLoad() method for
     * each sub-step.
     */
    public final void postLoad(ActionDescriptor action) {
        ActionDescriptor.checkNotNull("fcli cmd", cmd, this);
        forEach.postLoad(action);
    }
    
    /**
     * This class describes an fcli forEach element, allowing iteration over the output of
     * the fcli command. 
     */
    @Reflectable @NoArgsConstructor
    @Data @EqualsAndHashCode(callSuper = true)
    public static final class ActionStepFcliForEachDescriptor extends AbstractActionStepForEachDescriptor {
        protected final void _postLoad(ActionDescriptor action) {}
    }
}