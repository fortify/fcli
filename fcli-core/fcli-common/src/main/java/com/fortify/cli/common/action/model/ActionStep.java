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
    @JsonPropertyDescription("Optional: Execute one or more REST requests.")
    @JsonProperty(required = false) private List<ActionStepRequest> requests;
    
    @JsonPropertyDescription("Optional: Execute one or more fcli commands. For now, only fcli commands that support the standard output options (--output/--store/--to-file) may be used, allowing the JSON output of those commands to be used in subsequent or nested steps. Any console output is suppressed, and any non-zero exit codes will produce an error.")
    @JsonProperty(required = false) private List<ActionStepFcli> fcli;
    
    @JsonPropertyDescription("Optional: Write a progress message.")
    @JsonProperty(required = false) private TemplateExpression progress;
    
    @JsonPropertyDescription("Optional: Write a warning message to console and log file (if enabled). Note that warning messages will be shown on console only after all action steps have been executed, to not interfere with progress messages.")
    @JsonProperty(required = false) private TemplateExpression warn;
    
    @JsonPropertyDescription("Optional: Write a debug message to log file (if enabled).")
    @JsonProperty(required = false) private TemplateExpression debug;

    @JsonPropertyDescription("Optional: Throw an exception, thereby terminating action execution.")
    @JsonProperty(value = "throw", required = false) private TemplateExpression _throw;
    
    @JsonPropertyDescription("Optional: Terminate action execution and return the given exit code.")
    @JsonProperty(value = "exit", required = false) private TemplateExpression _exit;
    
    @JsonPropertyDescription("Optional: Set a data value for use in subsequent steps.")
    @JsonProperty(required = false) private List<ActionStepSet> set;
    
    @JsonPropertyDescription("Optional: Append a data value for use in subsequent steps.")
    @JsonProperty(required = false) private List<ActionStepAppend> append;
    
    @JsonPropertyDescription("Optional: Unset a data value for use in subsequent steps.")
    @JsonProperty(required = false) private List<ActionStepUnset> unset;
    
    @JsonPropertyDescription("Optional: Write data to a file, stdout, or stderr. Note that output to stdout and stderr will be deferred until action termination as to not interfere with progress messages.")
    @JsonProperty(required = false) private List<ActionStepWrite> write;
    
    @JsonPropertyDescription("Optional: Iterate over a given array of values.")
    @JsonProperty(required = false) private ActionStepForEach forEach;
    
    @JsonPropertyDescription("Optional: Mostly used for security policy and similar actions to define PASS/FAIL criteria. Upon action termination, check results will be written to console and return a non-zero exit code if the outcome of on or more checks was FAIL.")
    @JsonProperty(required = false) private List<ActionStepCheck> check;
    
    @JsonPropertyDescription("Optional: Sub-steps to be executed; useful for grouping or conditional execution of multiple steps.")
    @JsonProperty(required = false) private List<ActionStep> steps;
    
    /**
     * This method is invoked by the parent element (which may either be another
     * step element, or the top-level {@link Action} instance).
     * It invokes the postLoad() method on each request descriptor.
     */
    public final void postLoad(Action action) {}
}