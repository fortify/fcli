package com.fortify.cli.common.spring.expression;

import java.lang.reflect.Method;

import org.springframework.expression.spel.support.SimpleEvaluationContext;

public final class SpELHelper {
    public static final void registerFunctions(SimpleEvaluationContext context, Class<?> clazz) {
        for ( Method m : clazz.getDeclaredMethods() ) {
            context.setVariable(m.getName(), m);
        }
    }
}
