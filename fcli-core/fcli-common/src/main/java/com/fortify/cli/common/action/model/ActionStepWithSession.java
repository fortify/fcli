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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'with:session' step, making a session available for the
 * steps specified in the do-block, and cleaning up the session afterwards.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonTypeName("with-session")
@JsonClassDescription("Define session login and logout commands, respectively executed before and after the steps listed in the `do` block.")
@SampleYamlSnippets("""
    steps:
        - with:
            sessions:
            - login: fcli fod session login ... --fod-session=myActionSession
                logout: fcli fod session logout --fod-session=myActionSession
        do:
            - run.fcli:  
                myCmd: fcli fod ... --fod-session-myActionSession
        """)
public final class ActionStepWithSession extends AbstractActionElementIf {
    @JsonPropertyDescription("""
        Required SpEL template expression; the session login command to run before running \
        the steps specified in the do-block.
        """)
    @JsonProperty(value = "login", required = true) private TemplateExpression loginCommand;
    
    @JsonPropertyDescription("""
        Required SpEL template expression; the session logout command to run after the steps \
        in the do-block have been completed either successfully or with failure.
        """)
    @JsonProperty(value = "logout", required = true) private TemplateExpression logoutCommand;
    
    @Override
    public void postLoad(Action action) {
        Action.checkNotNull("login", this, loginCommand);
        Action.checkNotNull("logout", this, logoutCommand);
    }
    
    
}