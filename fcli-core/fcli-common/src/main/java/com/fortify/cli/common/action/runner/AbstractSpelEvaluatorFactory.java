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
package com.fortify.cli.common.action.runner;

import java.util.Collection;
import java.util.function.BiConsumer;

import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.spel.IConfigurableSpelEvaluator;
import com.fortify.cli.common.spel.SpelEvaluator;
import com.fortify.cli.common.spel.SpelHelper;

import lombok.Getter;

public abstract class AbstractSpelEvaluatorFactory {
    private boolean inConfigurationPhase = false;
    @Getter(lazy=true) private final IConfigurableSpelEvaluator spelEvaluator = createSpelEvaluator();
    
    private final IConfigurableSpelEvaluator createSpelEvaluator() {
        if ( inConfigurationPhase ) {
            throw new FcliBugException(this.getClass().getSimpleName()+"::getSpelEvaluator may not be invoked during configuration phase");
        }
        inConfigurationPhase = true;
        return SpelEvaluator.JSON_GENERIC.copy().configure(this::_configureSpelContext);
    }
    private final void _configureSpelContext(SimpleEvaluationContext spelContext) {
        SpelHelper.registerFunctions(spelContext, ActionSpelFunctions.class);
        configureSpelContext(spelContext);
    }
    protected <T> void configureSpelContext(SimpleEvaluationContext spelContext, Collection<BiConsumer<T, SimpleEvaluationContext>> configurers, T arg) {
        if ( configurers!=null ) {
            configurers.forEach(c->c.accept(arg, spelContext));
        }
    }
    protected abstract void configureSpelContext(SimpleEvaluationContext spelContext);
}