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

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Abstract base class for action elements supporting control flow:
 * conditional execution (if), error handling (on.fail), and success 
 * handling (on.success). Applies to both action steps and value handler 
 * elements like TemplateExpressionWithFormatter.
 * 
 * @author Ruud Senden
 */
@Reflectable @NoArgsConstructor
@Data 
public abstract class AbstractActionStepElement implements IActionStepElement {
    @JsonPropertyDescription("""
        Optional SpEL template expression: Only execute this instruction if the given if-expression evaluates to 'true'
        """)
    @ActionStepControlInstruction
    @JsonProperty(value = "if", required = false) private TemplateExpression _if;
    
    @JsonPropertyDescription("""
        Optional list: Steps to be executed if this element's execution fails, for example
        due to an exception or step-specific failure. \
        If not specified, the exception will propagate and action execution will terminate \
        (fail-fast behavior). If specified, failure of the current step will not automatically 
        result in action execution termination, but `on.fail` steps may for example include the 
        `throw` step to (conditionally) rethrow the exception. 
        
        Steps executed in `on.fail` can access exception details via the following properties:
        - `lastException.type`: Java simple class name of the exception
        - `lastException.message`: The exception message (the exception object, e.g., ${lastException.message}, ${lastException.getClass().simpleName}). \
        - `lastException.pojo`: The Java exception object itself, for example for use in `log.*` or `throw` steps
        For named elements, '<name>_exception' is also available with the same properties.
        """)
    @ActionStepControlInstruction
    @JsonProperty(value = "on.fail", required = false) private ArrayList<ActionStep> onFail;
    
    @JsonPropertyDescription("""
        Optional list: Steps to be executed if this element's execution succeeds.
        """)
    @ActionStepControlInstruction
    @JsonProperty(value = "on.success", required = false) private ArrayList<ActionStep> onSuccess;
}