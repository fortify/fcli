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

import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes default values for various action properties.
 */
@Reflectable @NoArgsConstructor
@Data
@JsonTypeName("config")
@JsonClassDescription("Define configuration settings for this action.")
@SampleYamlSnippets("""
        config:
          rest.target.default: fod
          output: immediate
        """)
public final class ActionConfig implements IActionElement {
    @JsonPropertyDescription("""
        Optional string: Default target to use for rest.call steps.    
        """)
    @JsonProperty(value = "rest.target.default", required = false) private String restTargetDefault;
    
    @JsonPropertyDescription("""
        (PREVIEW) Optional boolean: Default value for 'status.check' in 'run.fcli' instructions.
        
        For now, this instruction is meant to be used only by built-in fcli actions; custom actions \
        using this instruction may fail to run on other fcli 3.x versions.    
    """)
    @JsonProperty(value = "run.fcli.status.check.default", required = false) private Boolean runFcliStatusCheckDefault;
    
    @JsonPropertyDescription("""
        (PREVIEW) Optional boolean: Default value for 'status.log' in 'run.fcli' instructions.
        
        For now, this instruction is meant to be used only by built-in fcli actions; custom actions \
        using this instruction may fail to run on other fcli 3.x versions.    
    """)
    @JsonProperty(value = "run.fcli.status.log.default", required = false) private Boolean runFcliStatusLogDefault;
    
    @JsonPropertyDescription("""
        (PREVIEW) Optional string: Default value for 'group' in 'run.fcli' instructions.
        
        For now, this instruction is meant to be used only by built-in fcli actions; custom actions \
        using this instruction may fail to run on other fcli 3.x versions.    
    """)
    @JsonProperty(value = "run.fcli.group.default", required = false) private String runFcliGroupDefault;
    
    @JsonPropertyDescription("""
        Optional enum value: If set to 'delayed' (default), all output to stdout/stderr except for progress \
        messages will be delayed until the end of action execution. If set to 'immediate', output will be \
        written immediately and progress writer will be configured to 'simple' mode (unless '--progress=none' \
        is specified by the user) to avoid such output from interfering with progress messages.    
        """)
    @JsonProperty(value = "output", required = false) private ActionConfigOutput output = ActionConfigOutput.delayed;
    
    @JsonPropertyDescription("""
            Optional enum value: If set to 'include' (default), this action is included as an MCP tool on \
            the `fcli util mcp-server start` command.  If set to 'exclude' this action won't be available \
            as an MCP tool.   
            """)
    @JsonProperty(value = "mcp", required = false) private ActionMcpIncludeExclude mcp = ActionMcpIncludeExclude.include;
    
    @JsonPropertyDescription("""
        Optional map: Environment variables used by this action for which values should be masked in the fcli \
        log file. Map keys define environment variables names, map values define masking configuration.    
        """)
    @JsonProperty(value = "mask.env-vars", required = false) private LinkedHashMap<String, ActionInputMask> envVarMasks;
    
    @Override
    public void postLoad(Action action) {}
    
    public enum ActionConfigOutput {
        immediate, delayed
    }
    
    public enum ActionConfigSessionFromEnvOutput {
        show, suppress
    }
}