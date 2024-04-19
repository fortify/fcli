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

import java.util.Collection;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'check' operation, mostly useful for actions that
 * perform security gate or other checks.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionStepCheck implements IActionStep {
    /** Optional if-expression, executing this step only if condition evaluates to true */
    @JsonProperty("if") private TemplateExpression _if;
    /** Required display name for this check */
    private String displayName;
    /** Fail the check if condition evaluates to true. Either failIf or passIf must be specified. */
    private TemplateExpression failIf;
    /** Fail the check if condition evaluates to false. Either failIf or passIf must be specified. */
    private TemplateExpression passIf;
    /** Check result if check is not being executed due to no elements in forEach block
     *  or explicit if-statements on this step or one of its parents. */
    private CheckStatus ifSkipped = CheckStatus.SKIP;
    
    public final void postLoad(Action action) {
        Action.checkNotBlank("check displayName", displayName, this);
        Action.throwIf(failIf==null && passIf==null, this, ()->"Either passIf or failIf must be specified on check step");
        Action.throwIf(failIf!=null && passIf!=null, this, ()->"Only one of passIf or failIf may be specified on check step");
    }
    
    public static enum CheckStatus {
        PASS, FAIL, SKIP;
        
        public static CheckStatus combine(CheckStatus... statuses) {
            if ( Stream.of(statuses).anyMatch(FAIL::equals) ) { 
                return FAIL;
            } else if ( Stream.of(statuses).anyMatch(PASS::equals) ) {
                return PASS;
            } else {
                return SKIP;
            }
        }
        
        public static CheckStatus combine(Collection<CheckStatus> statusCollection) {
            return combine(statusCollection.toArray(CheckStatus[]::new));
        }
    }
}