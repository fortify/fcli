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
package com.fortify.cli.common.spel;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fortify.cli.common.spel.wrapper.SimpleExpression;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

public final class SpelHelper {
    private static final SpelExpressionParser parser = new SpelExpressionParser();
    private static final TemplateParserContext templateContext = new TemplateParserContext("${","}");
    
    public static final SimpleExpression parseSimpleExpression(String s) {
        return new SimpleExpression(s, parser.parseExpression(s));
    }
    public static final TemplateExpression parseTemplateExpression(String s) {
        return new TemplateExpression(s, parser.parseExpression(s, templateContext));
    }
    public static final void registerFunctions(SimpleEvaluationContext context, Class<?> clazz) {
        for ( Method m : clazz.getDeclaredMethods() ) {
            if ( Modifier.isPublic(m.getModifiers()) ) {
                context.setVariable(m.getName(), m);
            }
        }
    }
}
