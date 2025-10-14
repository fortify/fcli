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

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;
import com.fortify.cli.common.spel.SpelHelper;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;
import com.fortify.cli.common.util.OutputHelper.OutputType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'run.fcli' element.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonTypeName("run.fcli")
@JsonClassDescription("""
        Define an fcli command to be (optionally) executed. \
        This can be supplied as either a set of YAML properties or as a plain expression, 
        in which case the expression outcome is interpreted as the fcli command to run,
        with default values for all other properties.
        """)
@SampleYamlSnippets({"""
        steps:
        - run.fcli: 
            list-av: fcli ssc av ls
        ""","""
        steps:
        - run.fcli:
            avList:
                cmd: ssc av ls
                records.collect: true
        - log.debug: ${avList.records}
        ""","""
        steps:
        - run.fcli:
            process-av:
                cmd: ssc av ls
                records.for-each:
                record.var-name: av
                do:
                    - log.debug: ${av}
        """})
public final class ActionStepRunFcliEntry extends AbstractActionElementIf implements IMapKeyAware<String> {
    @JsonIgnore private String key;
    
    /** Allow for deserializing from a string that specified the fcli command to run, rather than object */
    public ActionStepRunFcliEntry(String cmdString) {
        this(SpelHelper.parseTemplateExpression(cmdString));
    }
    
    public ActionStepRunFcliEntry(TemplateExpression cmd) {
        this.cmd = cmd;
    }
    
    @JsonPropertyDescription("""
        (PREVIEW) Optional list of SpEL template expression: List entries define optional \
        skip reasons; if any of the given expressions evaluates to a non-blank string, this \
        fcli invocation will be skipped and the (first) non-blank skip reason will be logged.
        
        For now, this instruction is meant to be used only by built-in fcli actions; custom actions \
        using this instruction may fail to run on other fcli 3.x versions.
        """)
    @JsonProperty(value = "skip.if-reason", required = false) private ArrayList<TemplateExpression> skipIfReason;
    
    @JsonPropertyDescription("""
        (PREVIEW) Optional string: Define a group name for this fcli invocation. If defined, the output variables \
        for this fcli invocation will be added to an action variable named groupName.fcliIdentifier. For \
        example, given fcli invocation identifiers (map keys) CMD1 and CMD2, both specifying 'group: myGroup', the \
        myGroup.CMD1 action variable will contain the output variables (like skipped, exitCode, ...) for \
        CMD1, and the myGroup.CMD2 action variable will contain the output variables for CMD2, independent of whether \
        these fcli invocations were skipped, failed, or successful. 
        
        This can be used to iterate over all fcli invocations in a given group using 
        'records.for-each: from: ${#properties(myGroup)}', with the do-block for example referencing \
        ${groupEntry.status}.
            
        For now, this instruction is meant to be used only by built-in fcli actions; custom actions \
        using this instruction may fail to run on other fcli 3.x versions.
        """)
    @JsonProperty(value = "group", required = false) private String group;
    
    @JsonPropertyDescription("""
        Required SpEL template expression: The fcli command to run. This can be \
        specified with or without the 'fcli' command itself. Some examples:
        
        ssc appversion get --av ${av.id} --embed=attrValuesByName
        
        fcli fod rel ls
        """)
    @JsonProperty(value = "cmd", required = true) private TemplateExpression cmd;
    
    @JsonPropertyDescription("""
        Optional enum value: Specify how to handle output written to stdout by this fcli command:
            
        suppress:       Suppress fcli output to stdout
        collect:        Collect stdout output in an action variable, for example x.stdout
        show:           Show fcli stdout output
        
        Note that depending on the config:output setting, 'show' will either show the output \
        immediately, or output is delayed until action processing has completed.
        
        Default: 'suppress' if output is being processed through another instruction \
        ('records.for-each', 'records.collect', 'stdout.parser'), 'show' otherwise.
        """)
    @JsonProperty(value = "stdout", required = false) private OutputType stdoutOutputType;
    
    @JsonPropertyDescription("""
        Optional enum value: Specify how to handle output written to stderr by this fcli command:
                
        suppress:       Suppress fcli output to stderr
        collect:        Collect stderr output in an action variable, for example x.stderr
        show:           Show fcli stderr output
        
        Note that depending on the config:output setting, 'show' will either show the output \
        immediately, or output is delayed until action processing has completed.
        
        Default value: 'show'
        """)
    @JsonProperty(value = "stderr", required = false) private OutputType stderrOutputType;
    
    @JsonPropertyDescription("""
        Optional list: Steps to be executed if the fcli command was completed successfully. \
        Steps can reference the usual action variables generated by the fcli invocation to \
        access any records, stdout/stderr output, and exit code produced by the fcli command.
        """)
    @JsonProperty(value = "on.success", required = false) private ArrayList<ActionStep> onSuccess; 
    
    /* I don't think this can happen, as command exceptions will be caught by picocli and
     * result in a non-zero exit code, which is already handled by onFail below.
    @JsonPropertyDescription("""
        Optional list: Steps to be executed if the fcli command throws an exception. If not \
        specified, an exception will be thrown and action execution will terminate. Steps can \
        reference a variable named after the identifier for this fcli invocation, for example \
        'x_exception', to access the Java Exception object that represents the failure that occurred.
        """)
    @JsonProperty(value = "on.exception", required = false) private List<ActionStep> onException;
    */   
    
    @JsonPropertyDescription("""
        Optional list: Steps to be executed if the fcli command returns a non-zero exit code. \
        If not specified, an exception will be thrown and action execution will terminate if \
        the fcli command returned a non-zero exit code, unless 'status' is configured to ignore \
        exit status. Steps can reference the usual action variables generated by the fcli invocation \
        to access any records, stdout/stderr output, and exit code produced by the fcli command.
        """)
    @JsonProperty(value = "on.fail", required = false) private ArrayList<ActionStep> onFail;  
    
    @JsonPropertyDescription("""
        (PREVIEW) Optional boolean value, indicating whether exit status of the fcli command should be checked:
        
        true: Terminate action execution if the fcli command returned a non-zero exit code
        false: Continue action execution if the fcli command returned a non-zero exit code
        
        Default value is taken from 'config:run.fcli.status.status.check.default'. If not \
        specified, default value is 'false' if 'on.fail' is specified, 'true' otherwise.
        
        For now, this instruction is meant to be used only by built-in fcli actions; custom actions \
        using this instruction may fail to run on other fcli 3.x versions.
        """)
    @JsonProperty(value = "status.check", required = false) private Boolean statusCheck; 
    
    @JsonPropertyDescription("""
        (PREVIEW) Optional boolean value, indicating whether exit status of the fcli command should be logged:
        
        true: Output an informational message showing exit status
        false: Don't output an informational message showing exit status
        
        Default value is taken from 'config:run.fcli.status.status.log.default'. If not \
        specified, default value is 'false'.
        
        For now, this instruction is meant to be used only by built-in fcli actions; custom actions \
        using this instruction may fail to run on other fcli 3.x versions.
        """)
    @JsonProperty(value = "status.log", required = false) private Boolean statusLog; 
    
    @JsonPropertyDescription("""
        Optional boolean: If set to 'true', records produced by this fcli command will \
        be collected and accessible through an action variable named after the 'run.fcli' \
        identifier/map key.
        """)
    @JsonProperty(value = "records.collect", required = false) private boolean collectRecords;
    
    @JsonPropertyDescription("""
        Optional object: For fcli commands that produce records, this allows for running the \
        steps specified in the 'do' instruction for each individual record.
        """)
    @JsonProperty(value = "records.for-each", required = false) private ActionStepRunFcliEntry.ActionStepFcliForEachDescriptor forEachRecord;
    
    /**
     * This method is invoked by the {@link ActionStep#postLoad()}
     * method. It checks that required properties are set, then calls the postLoad() method for
     * each sub-step.
     */
    public final void postLoad(Action action) {
        Action.checkNotNull("fcli cmd", cmd, this);
    }
    
    /**
     * This class describes an fcli forEach element, allowing iteration over the output of
     * the fcli command. 
     */
    @Reflectable @NoArgsConstructor
    @Data @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("run.fcli-for-each")
    public static final class ActionStepFcliForEachDescriptor extends AbstractActionElementForEachRecord {
        protected final void _postLoad(Action action) {}
    }
}