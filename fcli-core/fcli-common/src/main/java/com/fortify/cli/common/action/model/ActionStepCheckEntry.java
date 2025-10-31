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
import java.util.Collection;
import java.util.HashSet;

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
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'check' operation, mostly useful for actions that
 * perform security gate or other checks.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonTypeName("check")
@JsonClassDescription("Define a (policy) check to be evaluated.")
@SampleYamlSnippets("""
        steps:
          - check:
              MY_CHECK:
                failIf: ${condition}
                ifSkipped: PASS
        """)
public final class ActionStepCheckEntry extends AbstractActionElementIf implements IMapKeyAware<String> {
    // Shared property description for passIf/failIf
    private static final String PASS_FAIL_IF = """
        Either 'passIf' or 'failIf' must be defined, both taking an SpEL template expression that \
        evaluates to 'true' or 'false'. 
        
        For 'passIf', the outcome of the check will be 'PASS' if the given expression evaluates to \
        'true', or 'FAIL' is the given expression evaluates to 'false'.
        
        For 'failIf', the outcome of the check will be 'FAIL' if the given expression evaluates to \
        'true', or 'PASS' is the given expression evaluates to 'false'.
        """;
    
    // Map key under which this instance was declared
    @JsonIgnore private String key;
    
    @JsonPropertyDescription("""
        Optional string: Display name of this check, to be displayed in PASS/FAIL messages. \
        If not defined, the display name will be set to the map key under which this check \
        is defined.
        """)
    @JsonProperty(value = "display-name", required = true) private String displayName;
    
    @JsonPropertyDescription(PASS_FAIL_IF)
    @JsonProperty(value = "fail.if", required = false) private TemplateExpression failIf;
    
    @JsonPropertyDescription(PASS_FAIL_IF)
    @JsonProperty(value = "pass.if", required = false) private TemplateExpression passIf;
    
    @JsonPropertyDescription("""
        Optional enum value: Define the check result in case the check is being skipped due to \
        conditional execution or no records to be processed in forEach blocks. Allowed values:
        FAIL: Fail the check if it was not executed
        PASS: Pass the check if it was not executed
        SKIP: Report that the test was skipped
        HIDE: Hide the check from output
        """)
    @JsonProperty(value = "ifSkipped", required = false, defaultValue = "SKIP") private CheckStatus ifSkipped = CheckStatus.SKIP;
    
    public final void postLoad(Action action) {
        if ( StringUtils.isBlank(displayName) ) { displayName = key; }
        Action.throwIf(failIf==null && passIf==null, this, ()->"Either pass.if or fail.if must be specified on check step");
        Action.throwIf(failIf!=null && passIf!=null, this, ()->"Only one of pass.if or fail.if may be specified on check step");
    }
    
    public static enum CheckStatus {
        // Statuses must be defined in order of precedence when combining,
        // i.e., when combining PASS and FAIL, outcome should be FAIL, so
        // FAIL should come before PASS.
        FAIL, PASS, SKIP, HIDE;
        
        public static CheckStatus combine(CheckStatus... statuses) {
            return combine(statuses==null ? null : Arrays.asList(statuses));
        }
        
        public static CheckStatus combine(Collection<CheckStatus> statuses) {
            if ( statuses==null ) { return null; }
            var set = new HashSet<CheckStatus>(statuses);
            for ( var s: values() ) {
                if ( set.contains(s) ) { return s; }
            }
            // Can only happen if all statuses are null
            return null;
        }
    }
}