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

import com.fortify.cli.common.spel.wrapper.TemplateExpression;

/**
 * Interface providing control flow methods for action elements.
 * Supports conditional execution (if), error handling (on.fail), 
 * and success handling (on.success) for both action steps and 
 * value handler elements like TemplateExpressionWithFormatter.
 * 
 * @author Ruud Senden
 */
public interface IActionStepElement extends IActionElement {
    /**
     * @return Optional conditional expression. Element only executes if this evaluates to true.
     */
    TemplateExpression get_if();
    
    /**
     * @return Optional list of steps to execute if this element's execution throws an exception.
     *         If undefined, exceptions propagate up (fail-fast behavior).
     */
    ArrayList<ActionStep> getOnFail();
    
    /**
     * @return Optional list of steps to execute if this element's execution succeeds.
     */
    ArrayList<ActionStep> getOnSuccess();
}
