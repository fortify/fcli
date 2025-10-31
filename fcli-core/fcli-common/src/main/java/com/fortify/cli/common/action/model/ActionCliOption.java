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

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes a action parameter.
 */
@Reflectable @NoArgsConstructor
@Data 
@JsonTypeName("cli.option")
@JsonClassDescription("Define command-line options supported by this action.")
@SampleYamlSnippets("""
        cli.options:
          file: # Can be referenced through ${cli.file} in action steps
            names: -f,--file
            description: Output file name
            required: false
            default: somefile.txt
        """)
public final class ActionCliOption implements IActionElement, IMapKeyAware<String> {
    @JsonIgnore private String key;
    
    @JsonPropertyDescription("""
        Required string: The option names allowed on the command line to specify a value \
        for this option. Multi-letter option names should be preceded by double dashes like \
        --option-name, single-letter option names should be preceded by a single dash like\
        -o. Multiple option names may be separated by a comma and optional whitespace, for \
        example:
        options: --option, -o
        """)
    @JsonProperty(value = "names", required = true) private String names;
    
    @JsonPropertyDescription("""
        Required string: Action parameter description to be shown in action usage help.    
        """)
    @JsonProperty(value = "description", required = true) private String description;
    
    @JsonPropertyDescription("""
        Optional string: Action parameter type: string (default), boolean, int, long, double, float, or array.
        """)
    @JsonProperty(value = "type", required = false) private String type;
        
    @JsonPropertyDescription("""
        Optional SpEL template expression: Default value for this CLI option if no value is specified by the user. \
        For example, this can be used to read a default value from an environment variable using ${#env('ENV_NAME')}  
        """)
    @JsonProperty(value = "default", required = false) private TemplateExpression defaultValue;
    
    @JsonPropertyDescription("""
        Optional boolean: CLI options are required by default, unless this property is set to false.    
        """)
    @JsonProperty(value = "required", required = false, defaultValue = "true") private boolean required = true;
    
    @JsonPropertyDescription("""
        Optional object: Mask option value in the fcli log file using the given mask configuration.
        """)
    @JsonProperty(value = "mask", required = false) private ActionInputMask mask;
    
    @JsonPropertyDescription("""
        Optional string: Allows for defining groups of options, which can for example be used with \
        ${#action.copyParametersFromGroup("optionGroupName")} 
        """)
    @JsonProperty(value = "group", required = false) private String group;
    
    @JsonPropertyDescription("""
            Optional enum value: If set to 'include' (default), this CLI option is included as an MCP tool \
            argument. If set to 'exclude', this CLI option is not included as an MCP tool argument. Only \
            non-required options may be excluded as MCP tool arguments. Also see `config::mcp` property to \
            include/exclude the action itself as an MCP tool.
            """)
    @JsonProperty(value = "mcp", required = false) private ActionMcpIncludeExclude mcp = ActionMcpIncludeExclude.include;
    
    @JsonIgnore public final String[] getNamesAsArray() {
        return names==null ? null : names.split("[\\s,]+");
    }
    
    public final void postLoad(Action action) {
        Action.checkNotBlank("CLI option names", getNames(), this);
        Action.checkNotNull("CLI option description", getDescription(), this);
        Arrays.stream(getNamesAsArray()).forEach(this::checkOptionName);
        // TODO Check no duplicate option names; ideally ActionRunner should also verify
        //      that option names/aliases don't conflict with command options
        //      like --help/-h, --log-file, ...
    }
    
    private final void checkOptionName(String optionName) {
        var validShortOptionName = optionName.length()==2 && optionName.charAt(0)=='-' && optionName.charAt(1)!='-';
        var validLongOptionName = optionName.length()>3 && optionName.startsWith("--") && optionName.charAt(3)!='-';
        Action.throwIf(!(validShortOptionName || validLongOptionName), this, ()->"Not a valid option name: "+optionName);
    }
}