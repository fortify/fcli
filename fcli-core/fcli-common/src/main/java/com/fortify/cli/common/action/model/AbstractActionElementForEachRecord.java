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
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This abstract class is the base class for forEach steps/properties.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public abstract class AbstractActionElementForEachRecord extends AbstractActionElementIf {
    @JsonPropertyDescription("""
        Required string: Variable name to assign to each individual record being processed, allowing \
        the record to be accessed by other instructions like 'if' or 'breakIf' and the steps defined \
        in the 'do' block through the specified variable name.
        """)
    @JsonProperty(value = "record.var-name", required = true) private String varName;
    
    @JsonPropertyDescription("""
        Required list: Steps to be executed for each individual record.
        """)
    @JsonProperty(value = "do", required = true)  private List<ActionStep> _do;
    
    @JsonPropertyDescription("""
        Optional SpEL template expression: Stop execution of the steps configured in the \
        'do' instruction if the breakIf expression evaluates to 'true'.
        """)
    @JsonProperty(value = "breakIf", required = false) private TemplateExpression breakIf;
    
    /**
     * This method is invoked by the {@link ActionStep#postLoad()}
     * method. It checks that required properties are set, then calls the postLoad() method for
     * each sub-step.
     */
    public final void postLoad(Action action) {
        Action.checkNotBlank("forEach record.var-name", varName, this);
        Action.checkNotNull("forEach do", _do, this);
    }
}