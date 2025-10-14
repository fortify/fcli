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
package com.fortify.cli.common.action.runner;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionsParseResult;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.spel.IConfigurableSpelEvaluator;
import com.fortify.cli.common.spel.ISpelEvaluator;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Singular;

/**
 * This class holds action runner configuration
 * @author Ruud Senden
 */
@Builder @Getter
public class ActionRunnerConfig {
    /** Progress writer instance */
    @NonNull private final IProgressWriterI18n progressWriter;
    /** Action to run */
    @NonNull private final Action action;
    /** Callback to handle validation errors */
    @NonNull private final Function<OptionsParseResult, RuntimeException> onValidationErrors;
    /** Action context configurers. Main purpose is to register request helpers on the context. */
    @Singular private final Collection<Consumer<ActionRunnerContext>> actionContextConfigurers;
    /** SpEL configuration functions for configuring the {@link ISpelEvaluator} instances provided by
     *  {@link ActionRunnerConfig} and {@link ActionRunnerContext} through their getSpelEvaluator()
     *  methods. Note that these configurers may not call getSpelEvaluator() during the configuration phase, 
     *  but they may register functions/classes that call getSpelEvaluator() */
    @Singular private final Collection<BiConsumer<ActionRunnerConfig, SimpleEvaluationContext>> actionConfigSpelEvaluatorConfigurers;
    /** SpEL configuration functions for configuring the {@link ISpelEvaluator} instance provided by
     *  {@link ActionRunnerContext}. Note that these configurers may not call the getSpelEvaluator() method 
     *  on {@link ActionRunnerContext} during the configuration phase, but they may call getSpelEvaluator()
     *  on the {@link ActionRunnerConfig} as returned by the {@link ActionRunnerContext#getConfig()} method. */
    @Singular private final Collection<BiConsumer<ActionRunnerContext, SimpleEvaluationContext>> actionContextSpelEvaluatorConfigurers;
    /** Default options to pass to fcli commands in run.fcli steps (if the fcli command supports that option) */ 
    @Singular private final Map<String,String> defaultFcliRunOptions;
    
    /** Factory for creating the single {@link ISpelEvaluator} instance. By using a factory, we can
     *  check for illegal access to the {@link ISpelEvaluator} during configuration phase. */
    @Getter(AccessLevel.NONE) private final ActionConfigSpelEvaluatorFactory spelEvaluatorFactory = new ActionConfigSpelEvaluatorFactory(this);
    public final IConfigurableSpelEvaluator getSpelEvaluator() {
        return spelEvaluatorFactory.getSpelEvaluator();
    }
    
    @RequiredArgsConstructor
    private static final class ActionConfigSpelEvaluatorFactory extends AbstractSpelEvaluatorFactory {
        private final ActionRunnerConfig config;
        
        protected final void configureSpelContext(SimpleEvaluationContext spelContext) {
            configureSpelContext(spelContext, config.getActionConfigSpelEvaluatorConfigurers(), config);
        }
    }
}