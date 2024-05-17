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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'check' operation, mostly useful for actions that
 * perform security gate or other checks.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public final class ActionStepCheck extends AbstractActionStep {
    @JsonPropertyDescription("Required string: Display name of this check, to be displayed in PASS/FAIL messages.")
    @JsonProperty(required = true) private String displayName;
    
    @JsonPropertyDescription("Required SpEL template expression if 'passIf' not specified: The outcome of this check will be 'FAIL' if the given expression evaluates to 'true', outcome will be 'PASS' otherwise.")
    @JsonProperty(required = false) private TemplateExpression failIf;
    
    @JsonPropertyDescription("Required SpEL template expression if 'failIf' not specified: The outcome of this check will be 'SUCCESS' if the given expression evaluates to 'true', outcome will be 'FAIL' otherwise.")
    @JsonProperty(required = false) private TemplateExpression passIf;
    
    @JsonPropertyDescription("Optional enum value: Define the check result in case the check is being skipped due to conditional execution or no records to be processed in forEach blocks.")
    @JsonProperty(required = false, defaultValue = "SKIP") private CheckStatus ifSkipped = CheckStatus.SKIP;
    
    public final void postLoad(Action action) {
        Action.checkNotBlank("check displayName", displayName, this);
        Action.throwIf(failIf==null && passIf==null, this, ()->"Either passIf or failIf must be specified on check step");
        Action.throwIf(failIf!=null && passIf!=null, this, ()->"Only one of passIf or failIf may be specified on check step");
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