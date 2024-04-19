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
 * This class describes a single action step element, which may contain 
 * requests, progress message, and/or set instructions. This class is 
 * used for both top-level step elements, and step elements in forEach elements. 
 * TODO Potentially, later versions may add support for other step types. Some ideas
 *      for potentially useful steps:
 *      <ul>
 *       <li>if: Execute sub-steps only if condition evaluates to true</li>
 *       <li>forEach: Execute sub-steps for every value in input array</li>
 *       <li>fcli: Run other fcli commands to allow for workflow-oriented templates.
 *           Primary question is what to do with output, i.e., store JSON output
 *           in 'data', ability to output regular command output to console (but
 *           how to avoid interference with ProgressWriter?), ...</li>
 *      </ul>
 *
 * @author Ruud Senden
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public final class ActionStep extends AbstractActionStep {
    /** Optional requests for this step element */
    private List<ActionStepRequest> requests;
    /** Optional fcli commands for this step element */
    private List<ActionStepFcli> fcli;
    /** Optional progress message template expression for this step element */
    private TemplateExpression progress;
    /** Optional warning message template expression for this step element */
    private TemplateExpression warn;
    /** Optional debug message template expression for this step element */
    private TemplateExpression debug;
    /** Optional exception message template expression for this step element */
    @JsonProperty("throw") private TemplateExpression _throw;
    /** Optional exit step element to generate exit code and terminate the action */
    @JsonProperty("exit") private TemplateExpression _exit;
    /** Optional set operations */
    private List<ActionStepSet> set;
    /** Optional add operations */
    private List<ActionStepAppend> append;
    /** Optional unset operations */
    private List<ActionStepUnset> unset;
    /** Optional write operations */
    private List<ActionStepWrite> write;
    /** Optional forEach operation */
    private ActionStepForEach forEach;
    /** Optional check operation */
    private List<ActionStepCheck> check;
    /** Optional sub-steps to be executed, useful for grouping or conditional execution */
    private List<ActionStep> steps;
    
    /**
     * This method is invoked by the parent element (which may either be another
     * step element, or the top-level {@link Action} instance).
     * It invokes the postLoad() method on each request descriptor.
     */
    public final void postLoad(Action action) {}
}