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

import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes a action parameter.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionParameter implements IActionElement {
    @JsonPropertyDescription("Required string: Action parameter name. This will allow the action to accept CLI options named `--[name]` or `-[name]` for single-letter names. Parameter value can be referenced through ${parameters.[name]} in SpEL template expressions.")
    @JsonProperty(required = true) private String name;
    
    @JsonPropertyDescription("Required string: Action parameter description to be shown in action usage help.")
    @JsonProperty(required = true) private String description;
    
    @JsonPropertyDescription("Optional string: Comma-separated CLI option aliases. This will allow the action to accept CLI options named `--[alias]` or `-[alias]` for single-letter aliases. Aliases cannot be referenced in SpEL expressions.")
    @JsonProperty(required = false) private String cliAliases;
    
    @JsonPropertyDescription("Optional string: Action parameter type. Supported types depends on the fcli module (SSC/FoD) from which the action is being run. See built-in actions for examples of supported types.")
    @JsonProperty(required = false) private String type;
    
    @JsonPropertyDescription("Optional map(string,SpEL template expression): Action parameter type parameters to allow for additional configuration of the type converter configured through 'type'.")
    @JsonProperty(required = false) private Map<String, TemplateExpression> typeParameters;
    
    @JsonPropertyDescription("Optional SpEL template expression: Default value for this action parameter if no value is specified by the user.")
    @JsonProperty(required = false) private TemplateExpression defaultValue;
    
    @JsonPropertyDescription("Optional boolean: All parameters are required by default, unless this property is set to false.")
    @JsonProperty(required = false, defaultValue = "true") private boolean required = true;
    
    public final void postLoad(Action action) {
        Action.checkNotBlank("parameter name", name, this);
        Action.checkNotNull("parameter description", getDescription(), this);
        // TODO Check no duplicate names; ideally ActionRunner should also verify
        //      that option names/aliases don't conflict with command options
        //      like --help/-h, --log-file, ...
    }
    
    public final String[] getCliAliasesArray() {
        if ( cliAliases==null ) { return new String[] {}; }
        return Stream.of(cliAliases).map(String::trim).toArray(String[]::new);
    }
}