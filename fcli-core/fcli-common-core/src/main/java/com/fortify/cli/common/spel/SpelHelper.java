/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.common.spel;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fortify.cli.common.spel.wrapper.SimpleExpression;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

public final class SpelHelper {
    private static final FcliSpelExpressionParser parser = new FcliSpelExpressionParser();
    
    public static final SimpleExpression parseSimpleExpression(String s) {
        return parser.parseSimpleExpression(s);
    }
    public static final TemplateExpression parseTemplateExpression(String s) {
        return parser.parseTemplateExpression(s);
    }
    public static final void registerFunctions(SimpleEvaluationContext context, Class<?> clazz) {
        for ( Method m : clazz.getDeclaredMethods() ) {
            if ( Modifier.isPublic(m.getModifiers()) ) {
                context.setVariable(m.getName(), m);
            }
        }
    }
}
