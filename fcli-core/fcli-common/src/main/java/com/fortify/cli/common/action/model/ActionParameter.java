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
    /** Required parameter name */
    private String name;
    /** Optional comma-separated CLI aliases */
    private String cliAliases;
    /** Required parameter description */
    private String description;
    /** Optional parameter type */
    private String type;
    /** Optional type parameters*/
    private Map<String, TemplateExpression> typeParameters;
    /** Optional template expression defining the default parameter value if not provided by user */
    private TemplateExpression defaultValue;
    /** Boolean indicating whether this parameter is required, default is true */
    private boolean required = true;
    
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