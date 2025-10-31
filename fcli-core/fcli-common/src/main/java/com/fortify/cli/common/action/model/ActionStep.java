/*
 * Copyright 2021-2025 Open Text.
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

/**
 * This class describes a single action step element, which may contain 
 * requests, progress message, and/or set instructions. This class is 
 * used for both top-level step elements, and step elements in forEach elements. 
 * @author Ruud Senden
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonTypeName("step")
@JsonClassDescription("Define a step to be executed by this action.")
@SampleYamlSnippets("""
        steps:
          - var.set:
              ...
          - rest.call:
              ...
          - if: ${expr}
            run.fcli:
              ...
        """)
public final class ActionStep extends AbstractActionElementIf {
    // Capture fields in this class annotated with @JsonProperty, indexed by JSON property name
    // Only used to initialize getters and propertyTypes 
    @JsonIgnore private static final Map<String, Field> fields = createFields();
    // Capture getter MethodHandles for all fields captured above, indexed by JSON property name
    @JsonIgnore private static final Map<String, MethodHandle> getters = createGetters(fields);
    // Capture field types for all fields captured above, indexed by JSON property name
    // Used by ActionStepProcessorSteps to look up the corresponding processor for each JSON property name
    @JsonIgnore @Getter private static final Map<String, Class<?>> propertyTypes = createPropertyTypes(fields);
    // Capture the single non-null property value, indexed by JSON property name
    @JsonIgnore private Map.Entry<String, Object> stepValue;
    
    @JsonPropertyDescription("""
        Set one or more variables values for use in later action steps. This step takes a list of variables to \
        set, with each list item taking a single yaml property that represents the variable name to set, which \
        may be specified as an SpEL template expression. 
        
        Based on the format of the variable name, this step can either set/replace a single-value variable, \
        set/replace a property on a variable containing a set of properties, or append a value to an array-type \
        variable. By default, variables are only accessible by the current fcli action, unless they are prefixed \
        with 'global.', in which case they are also accessible by other actions that execute within the context \
        of a single fcli command-line invocation. For example, if action 1 uses the run.fcli step to execute action \
        2, any global variables set in action 1 will be accessibly by action 2, and vice versa. 
        
        Following are some examples that show how to specify the operation to perform, and thereby implicitly \
        declaring the variable type:
        
        # Set/replace the single-value variable named 'var1'
        var1: ...
        
        # Set/replace properties 'prop1' and 'prop2' on variable 'var2'
        var2.prop1: ...
        var2.prop2: ...
        
        # Append two items to the array-type variable 'var3' (two trailing dots) 
        var3..: ...
        var3..: ...
        
        # Same as above, but setting global variables:
        global.var1: ...
        global.var2.prop1: ...
        global.var2.prop2: ...
        global.var3..: ...
        global.var3..: ...
        
        # The following would be illegal, as a variable cannot contain both
        # a set of properties and an array:
        var4.prop1: ...
        var4..: ...
        
        Due to this syntax, variable names cannot contain dots like 'var.1', as '1' would be \
        interpreted as a property name on the 'var' variable. Property names may contain dots \
        though, so 'var5.x.y' would be intepreted as property name 'x.y' on 'var5'.
        
        Values may be specified as either an SpEL template expression, or as 'value' and 'fmt' \
        properties. An 'if' property is also supported to conditionally set the variable. If \
        formatter is specified, the given formatter from the 'formatters' section will be used to \
        format the given value, or, if no value is given, the formatter will be evaluated against \
        the set of all variables. Some examples:
        
        global.name: John Doe
        simpleValue1: Hello ${global.name}
        formattedValue1: {fmt: myFormatter, if: "${someExpression}"}
        formatterValue2: {value: "${myVar}", fmt: "${myVarFormatterExpression}"}     
        
        Within a single 'var.set*' step, variables are processed in the order that they are \
        declared, allowing earlier declared variables to be referenced by variables or \
        formatters that are declared later in the same step.
        """)
    @SampleYamlSnippets(copyFrom=TemplateExpressionWithFormatter.class, value="""
        steps:
          - var.set:
              var1: Hello ${name}
              var2.p1: This is property 1 on var2
              var2.p2: This is property 2 on var2 
              var3..: This is element 1 on var3
          - var.set:
              var3..: This is element 2 on var3
        """)
    @JsonProperty(value = "var.set", required = false) private LinkedHashMap<TemplateExpression,TemplateExpressionWithFormatter> varSet;
    
    @JsonPropertyDescription("""
        Remove one or more local or global variables. Variable names to remove can be provided as plain \
        text or as a SpEL template expression, resolving to for example 'var1' or 'global.var2'.
        """)
    @SampleYamlSnippets("""
        steps:
          - var.rm:
              - var1    # Remove variable named 'var1'
              - ${var2} # Remove variable name as stored in var2 variable
        """)
    @JsonProperty(value = "var.rm", required = false) private List<TemplateExpression> varRemove;
    
    @JsonPropertyDescription("""
        Write a progress message. Progress messages are usually written to console and log \
        file directly. Depending on progress writer configuration, progress messages may \
        disappear when the next progress message is written, or after all action steps have \
        been executed. If you need to write an information message that is always displayed \
        to the end user, without the possibility of the message being removed, please use \
        log.info instead.     
        """)
    @SampleYamlSnippets("""
        steps:
          - log.progress: Processing record ${recordNumber}
        """)
    @JsonProperty(value = "log.progress", required = false) private TemplateExpression logProgress;
    
    @JsonPropertyDescription("""
        Write an informational message to console and log file (if enabled). Note that depending \
        on the config:output setting, informational messages may be shown either immediately, or \
        only after all action steps have been executed, to not interfere with progress messages.
        """)
    @SampleYamlSnippets("""
        steps:
          - log.info: Output written to ${fileName}
        """)
    @JsonProperty(value = "log.info", required = false) private TemplateExpression logInfo;
    
    @JsonPropertyDescription("""
        Write a warning message to console and log file (if enabled). Note that depending on the \
        config:output setting, warning messages may be shown either immediately, or only after all \
        action steps have been executed, to not interfere with progress messages.
        """)
    @SampleYamlSnippets("""
        steps:
          - log.warn: Skipping this part due to errors: ${errors}
        """)
    @JsonProperty(value = "log.warn", required = false) private TemplateExpression logWarn;
    
    @JsonPropertyDescription("Write a debug message to log file (if enabled).")
    @SampleYamlSnippets("""
        steps:
           - log.debug: ${#this}   # Log all action variables
         """)
    @JsonProperty(value = "log.debug", required = false) private TemplateExpression logDebug;
    
    @JsonPropertyDescription("""
        Add REST request targets for use in 'rest.call' steps. This step takes a map, with \
        keys defining REST target names, and values defining the REST target definition. 
        """)
    @SampleYamlSnippets(copyFrom = ActionStepRestTargetEntry.class)
    @JsonProperty(value = "rest.target", required = false) private LinkedHashMap<String, ActionStepRestTargetEntry> restTargets;
    
    @JsonPropertyDescription("""
        Execute one or more REST calls. This step takes a map, with keys defining an indentifier \
        for the REST call, and values defining the request data and how to process the response. \
        For paged REST requests, a single 'rest.call' instruction will execute multiple REST \
        requests to load the individual pages. The response of each individual REST request \
        will be stored as local action variables. For example, given a rest.call identifier 'x' \
        the following local action variables will be set:
        
        x: The processed response
        x_raw: The raw, unprocessed response
        x_exception: Java Exception instance if the request failed
        
        These variables can be referenced only within the current 'rest.call' map entry, for \
        example by 'log.progress', 'on.success', and 'records.for-each'. They are not accessible 
        by later steps or other map entries within the same 'rest.call' step. If you wish to make \
        any data produced by the REST call available to later steps, you'll need to use 'var.*' \
        steps in either 'on.success' or 'records.for-each' instructions.
         
        Note that multiple REST calls defined within a single 'rest.call' step will be executed \
        in the specified order, but the requests are built independent of each other. As such, \
        within a single 'rest.call' step, variables set by one 'rest.call' map entry cannot be \
        accessed in the request definition (uri, query, body, ...) of another map entry. The \
        reason is that for target systems that supports bulk requests (like SSC), multiple \
        requests within a single 'rest.call' instruction may be combined into a single bulk \
        request, so none of the REST responses will be available yet while building the bulk \
        request. If you need to use the output from one REST call as input for another REST \
        call, these REST calls should be defined in separate 'rest.call' steps.
        """)
    @SampleYamlSnippets(copyFrom = ActionStepRestCallEntry.class)
    @JsonProperty(value = "rest.call", required = false) private LinkedHashMap<String, ActionStepRestCallEntry> restCalls;
    
    @JsonPropertyDescription("""
        Execute one or more fcli commands. This step takes a map, with map keys defining an identifier \
        for the fcli invocation, and values defining the fcli command to run and how to process the \
        output and exit code. The identifier can be used in later steps (or later fcli invocations in \
        the same 'run.fcli' step) to access the output of the fcli command, like stdout, stderr, and \
        exit code that were produced by the fcli command, depending on step configuration. For example, 
        given an fcli invocation identifier named 'x', the following action variables may be set: 
        
        x.records: Array of records produced by the fcli invocation if 'records.collect' is set to 'true'
        x.stdout: Output produced on stdout by the fcli invocation if 'stdout' is set to 'collect'
        x.stderr: Output produced on stderr by the fcli invocation if 'stderr' is set to 'collect'
        x.exitCode: Exit code of the fcli invocation
        
        The following action variables may also be set by this fcli step, but these are considered \
        preview functionality and may be removed or renamed at any time. For now, these are meant to \
        be used only by built-in fcli actions; custom actions using these action variables may fail to \
        run on other fcli 3.x versions.
        
        x.skipped: Boolean value indicating whether execution was skipped due to skip.if-reason configuration
        x.skipReason: Reason why execution was skipped; will be null if x.skipped==true (deprecated; see x.statusReason)
        x.status: Set to either SKIPPED (x.skipped==true), SUCCESS (x.exitCode==0), or FAILED (x.exitCode!=0)
        x.statusReason: Reason why execution failed or was skipped; will be null if step was executed successfully
        x.dependencySkipReason: Optional skip reason for steps that are dependent on this fcli invocation
        x.success: Set to true if fcli invocation was successful, false if failed
        x.failed: Set to true if fcli invocation failed, false if successfull
        
        """)
    @SampleYamlSnippets(copyFrom = ActionStepRunFcliEntry.class)
    @JsonProperty(value = "run.fcli", required = false) private LinkedHashMap<String, ActionStepRunFcliEntry> runFcli;
    
    @JsonPropertyDescription("""
        Write data to a file, stdout, or stderr. This step takes a map, with map keys defining the destination, \
        and map values defining the data to write to the destination. Destination can be specified as either \
        stdout, stderr, or a file name. If a file already exists, it will be overwritten. Note that depending \
        on the config:output setting, data written to stdout or stderr may be shown either immediately, or only \
        after all action steps have been executed, to not interfere with progress messages.
        
        Map values may be specified as either an SpEL template expression, or as 'value' and 'fmt' \
        properties. An 'if' property is also supported to conditionally write to the output. If \
        formatter is specified, the given formatter from the 'formatters' section will be used to \
        format the given value, or, if no value is given, the formatter will be evaluated against \
        the set of all variables. Some examples:
        
        /path/to/myFile1: Hello ${name}
        /path/to/myFile2: {fmt: myFormatter, if: "${someExpression}"}
        /path/to/myFile3: {value: "${myVar}", fmt: "${myVarFormatterExpression}"}     
        """)
    @SampleYamlSnippets("""
        steps:
          - out.write:
              ${cli.file}: {fmt: output}    
        """)
    @JsonProperty(value="out.write", required = false) private LinkedHashMap<TemplateExpression, TemplateExpressionWithFormatter> outWrite;
    
    @JsonPropertyDescription("""
        Mostly used for security policy and similar actions to define PASS/FAIL criteria. Upon action termination, \
        check results will be written to console and return a non-zero exit code if the outcome of one or more checks \
        was FAIL. This instructions takes a map, with keys defining the check name, and values defining the check \
        definition. Current check status can be accessed through ${checkStatus.checkName}, for example allowing to \
        conditionally execute additional checks based on earlier check outcome. Note that if the same check name \
        (map key) is used in different 'check' steps, they will be treated as separate checks, and ${checkStatus.checkName} \
        will contain the status of the last executed check for the given check name.
        """)
    @SampleYamlSnippets(copyFrom = ActionStepCheckEntry.class)
    @JsonProperty(value = "check", required = false) private LinkedHashMap<String, ActionStepCheckEntry> check;
    
    @JsonPropertyDescription("""
        Execute the steps defined in the 'do' block for every record provided by the 'from' expression.    
        """)
    @SampleYamlSnippets(copyFrom = ActionStepRecordsForEach.class)
    @JsonProperty(value = "records.for-each", required = false) private ActionStepRecordsForEach recordsForEach;
    
    @JsonPropertyDescription("""
        This step allows for running initialization and cleanup steps around the steps listed in the 'do' block. \
        This includes the ability to run the do-block within the context of an fcli session, with the session \
        being created before running the do-block and being terminated afterwards, and the ability to define \
        writers than can output data in various formats like CSV, appending data to those writers in the do-block, \
        and closing those writers once the steps in the do-block have completed. Compared to the 'out.write' instruction, \
        these writers support more output formats, and, depending on writer type and configuration, allows for streaming \
        output, rather than having to collect all data in memory first.
        """)
    @SampleYamlSnippets(copyFrom = ActionStepWith.class)
    @JsonProperty(value = "with", required = false) private ActionStepWith with;
    
    @JsonPropertyDescription("""
        This instruction may only be used from within a with:do, with the with:writers instruction defining the writers \
        that the writer.append instruction can append data to. The given data will be formatted an written according to \
        the corresponding writer configuration.  
        """)
    @SampleYamlSnippets("""
        steps:
          - with:
              writers:
                csvWriter: ...
            do:
              - writer.append:
                  csvWriter: ${myCsvRecord}    
        """)
    @JsonProperty(value="writer.append", required = false) private LinkedHashMap<String, TemplateExpressionWithFormatter> writerAppend;
    
    @JsonPropertyDescription("""
        Sub-steps to be executed; useful for grouping or conditional execution of multiple steps.    
        """)
    @SampleYamlSnippets("""
        steps:
          - ...
          - if: ${condition}
            steps:
              - ... # One or more steps to execute if ${condition} evaluates to true     
        """)
    @JsonProperty(value = "steps", required = false) private List<ActionStep> steps;
    
    @JsonPropertyDescription("""
        Throw an exception, thereby terminating action execution.
        """)
    @SampleYamlSnippets("""
        steps:
          - throw: ERROR: ${errorMessage}    
        """)
    @JsonProperty(value = "throw", required = false) private TemplateExpression _throw;
    
    @JsonPropertyDescription("""
        Terminate action execution and return the given exit code.
        """)
    @SampleYamlSnippets("""
        steps:
          - if: ${someCondition}
            exit: 1    
        """)
    @JsonProperty(value = "exit", required = false) private TemplateExpression _exit;
    
    /**
     * This method is invoked by the parent element (which may either be another
     * step element, or the top-level {@link Action} instance).
     * It invokes the postLoad() method on each request descriptor.
     */
    public final void postLoad(Action action) {
        HashMap<String, Object> values = getters.entrySet().stream().collect(
                HashMap::new, this::putNonNullFieldValue, Map::putAll);
        if ( values.size()==0 ) {
            throw new FcliActionValidationException("Action step doesn't define any instruction");
        }
        if ( values.size()>1 ) {
            throw new FcliActionValidationException("Action step contains multiple instructions: "+values.keySet());
        }
        this.stepValue = values.entrySet().iterator().next();
    }

    @SneakyThrows
    private final void putNonNullFieldValue(HashMap<String, Object> map, Map.Entry<String, MethodHandle> e) {
        var value=e.getValue().invokeExact(this);
        if (value!=null) { map.put(e.getKey(), value); }
    }
    
    // Collect all fields annotated with @JsonProperty, indexed by JSON property name
    private static final Map<String, Field> createFields() {
        var result = new HashMap<String, Field>();
        for ( var f : ActionStep.class.getDeclaredFields() ) {
            var jsonPropertyAnnotation = f.getAnnotation(JsonProperty.class); 
            if ( jsonPropertyAnnotation!=null ) {
                var jsonPropertyName = jsonPropertyAnnotation.value();
                if ( StringUtils.isBlank(jsonPropertyName) ) {
                    throw new FcliBugException("JSON properties in ActionStep must define explicit property name");
                }
                result.put(jsonPropertyName, f);
            }
        }
        return result;
    }

    private static final HashMap<String, Class<?>> createPropertyTypes(Map<String, Field> fields) {
        return fields.entrySet().stream().collect(
                HashMap::new, (map,e)->map.put(e.getKey(), e.getValue().getType()), Map::putAll);
    }
    
    @JsonIgnore
    private static final Map<String, MethodHandle> createGetters(Map<String, Field> fields) {
        return fields.entrySet().stream().collect(
            HashMap::new, (map,e)->map.put(e.getKey(), createGetter(e.getValue())), Map::putAll);
    }
    
    @JsonIgnore
    private static final MethodHandle createGetter(Field f) {
        try {
            return MethodHandles.lookup().unreflectGetter(f).asType(MethodType.methodType(Object.class, ActionStep.class));
        } catch (IllegalAccessException e) {
            throw new FcliBugException("Unable to create getter for field "+f.getName(), e);
        }
    }
}