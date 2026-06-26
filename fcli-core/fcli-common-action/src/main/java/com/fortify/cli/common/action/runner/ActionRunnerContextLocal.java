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
package com.fortify.cli.common.action.runner;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.helper.ci.ActionCiSpelFunctionsRegistry;
import com.fortify.cli.common.action.helper.fs.ActionFileSystemSpelFunctions;
import com.fortify.cli.common.action.model.ActionStepCheckEntry;
import com.fortify.cli.common.action.model.ActionStepCheckEntry.CheckStatus;
import com.fortify.cli.common.action.model.FcliActionValidationException;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.rest.unirest.UnirestContext;
import com.fortify.cli.common.spel.IConfigurableSpelEvaluator;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * This class holds action execution context. Global state (config, writers, check statuses,
 * exit code) is shared across all child contexts via {@link ActionRunnerContextGlobal}.
 * Local state (vars, spelEvaluator, requestHelpers) is scoped per context instance.
 * @author Ruud Senden
 */
public class ActionRunnerContextLocal implements AutoCloseable {
    @Getter(AccessLevel.NONE)
    private final ActionRunnerContextGlobal global;
    @Getter private final ActionRunnerVars vars;
    @Getter(AccessLevel.NONE)
    private final IConfigurableSpelEvaluator spelEvaluator;
    private final Map<String, IActionRequestHelper> requestHelpers;
    private final Map<String, IRecordWriter> writers;
    @Getter private Function<JsonNode, Boolean> yieldConsumer;
    
    /** Root context constructor — used by {@link #create(ActionRunnerConfig, IProgressWriterI18n, ObjectNode)} */
    private ActionRunnerContextLocal(ActionRunnerContextGlobal global, IConfigurableSpelEvaluator spelEvaluator) {
        this.global = global;
        this.spelEvaluator = spelEvaluator;
        this.requestHelpers = new HashMap<>();
        this.writers = new HashMap<>();
        this.vars = new ActionRunnerVars(spelEvaluator, global.getParameterValues());
    }
    
    /** Child context constructor — used by {@link #createChild()} */
    private ActionRunnerContextLocal(ActionRunnerContextGlobal global, ActionRunnerVars vars,
            IConfigurableSpelEvaluator spelEvaluator, Map<String, IActionRequestHelper> requestHelpers,
            Map<String, IRecordWriter> writers) {
        this.global = global;
        this.vars = vars;
        this.spelEvaluator = spelEvaluator;
        this.requestHelpers = requestHelpers;
        this.writers = writers;
    }
    
    /**
     * Create a root {@link ActionRunnerContextLocal} for an action execution.
     */
    static ActionRunnerContextLocal create(ActionRunnerConfig config, IProgressWriterI18n progressWriter, ObjectNode parameterValues) {
        var global = new ActionRunnerContextGlobal(config, progressWriter, parameterValues);
        var spelEvaluatorFactory = new ActionConfigSpelEvaluatorFactory(global);
        var ctx = new ActionRunnerContextLocal(global, spelEvaluatorFactory.getSpelEvaluator());
        // Reconfigure SpEL context now that we have a full ActionRunnerContext
        // (needed for SpEL functions that reference the context)
        spelEvaluatorFactory.reconfigureWithContext(ctx);
        ctx.registerFnVariable();
        ctx.initialize();
        return ctx;
    }
    
    /**
     * Create a child context for loops (records.for-each). Child vars propagate to parent.
     * SpEL evaluator and request helpers are inherited (shared references).
     */
    public ActionRunnerContextLocal createChild() {
        return new ActionRunnerContextLocal(global, vars.createChild(), spelEvaluator, requestHelpers, new HashMap<>(writers));
    }
    
    /**
     * Create a child context for product blocks (with.product). Child vars propagate
     * to parent but use a copied SpEL evaluator so that product-specific SpEL variables
     * and request helpers don't leak to the parent scope.
     */
    public ActionRunnerContextLocal createChildForProduct(IActionProductContextProvider provider, String session) {
        var childSpel = spelEvaluator.copy();
        var childHelpers = new HashMap<>(requestHelpers);
        var childVars = vars.createChild();
        var child = new ActionRunnerContextLocal(global, childVars, childSpel, childHelpers, new HashMap<>(writers));
        child.registerFnVariable();
        childVars.setSpelEvaluator(childSpel);
        provider.configureSpelContext(childSpel, child, session);
        provider.configureActionContext(child, session);
        return child;
    }
    
    /**
     * Create a child context for function invocations. Uses isolated vars (no propagation
     * to parent), a copied SpEL evaluator, and injects the args as a local variable.
     */
    public ActionRunnerContextLocal createChildForFunction(ObjectNode argsNode) {
        var childVars = vars.createIsolatedChild();
        childVars.setLocal("args", argsNode);
        var childSpel = spelEvaluator.copy();
        var child = new ActionRunnerContextLocal(global, childVars, childSpel, new HashMap<>(requestHelpers), new HashMap<>(writers));
        child.registerFnVariable();
        childVars.setSpelEvaluator(childSpel);
        return child;
    }
    
    /**
     * Set the yield consumer for streaming function execution.
     */
    public void setYieldConsumer(Function<JsonNode, Boolean> yieldConsumer) {
        this.yieldConsumer = yieldConsumer;
    }
    
    /**
     * Register the #fn SpEL variable pointing to this context.
     */
    void registerFnVariable() {
        spelEvaluator.configure(spelCtx ->
            spelCtx.setVariable("fn", new ActionFunctionSpelFunctions(this)));
    }
    
    /**
     * Close request helpers that were added in this context but not present in the parent.
     * Used by with.product to clean up product-specific REST connections.
     */
    public void closeAddedRequestHelpers(ActionRunnerContextLocal parent) {
        requestHelpers.entrySet().stream()
            .filter(e -> !parent.requestHelpers.containsKey(e.getKey()))
            .forEach(e -> e.getValue().close());
    }
    
    public final ActionRunnerContextLocal initialize() {
        getConfig().getActionContextConfigurers().forEach(configurer->configurer.accept(this));
        var actionConfig = getConfig().getAction().getConfig();
        if ( actionConfig!=null && Boolean.TRUE.equals(actionConfig.getEphemeralEncrypt()) ) {
            FcliExecutionContextHolder.current().enableEphemeralEncryption();
        }
        return this;
    }
    
    // Delegate global state accessors
    public final ObjectMapper getObjectMapper() { return global.getObjectMapper(); }
    public final ActionRunnerConfig getConfig() { return global.getConfig(); }
    public final IProgressWriterI18n getProgressWriter() { return global.getProgressWriter(); }
    public final ObjectNode getParameterValues() { return global.getParameterValues(); }
    public final Map<ActionStepCheckEntry, CheckStatus> getCheckStatuses() { return global.getCheckStatuses(); }
    public final Map<String, IRecordWriter> getWriters() { return writers; }
    public final int getExitCode() { return global.getExitCode(); }
    public final void setExitCode(int exitCode) { global.setExitCode(exitCode); }
    public final boolean isExitRequested() { return global.isExitRequested(); }
    public final void setExitRequested(boolean exitRequested) { global.setExitRequested(exitRequested); }
    
    public final void addRequestHelper(String name, IActionRequestHelper requestHelper) {
        requestHelpers.put(name, requestHelper);
    }
    
    public final Map<String, IActionRequestHelper> getRequestHelpers() {
        return requestHelpers;
    }
    
    public final IActionRequestHelper getRequestHelper(String name) {
        if ( StringUtils.isBlank(name) ) {
            if ( requestHelpers.size()==1 ) {
                return requestHelpers.values().iterator().next();
            } else {
                throw new FcliActionValidationException(String.format("Required 'from:' property (allowed values: %s) missing", requestHelpers.keySet()));
            }
        } 
        var result = requestHelpers.get(name);
        if ( result==null ) {
            throw new FcliActionValidationException(String.format("Invalid 'from: %s', allowed values: %s", name, requestHelpers.keySet()));
        }
        return result;
    }
    
    public final IConfigurableSpelEvaluator getSpelEvaluator() {
        return spelEvaluator;
    }
    
    public final UnirestContext getUnirestContext() {
        return getConfig().getUnirestContext();
    }
    
    private static final class ActionConfigSpelEvaluatorFactory extends AbstractSpelEvaluatorFactory {
        private final ActionRunnerContextGlobal global;
        private ActionRunnerContextLocal actionRunnerContext;
        
        ActionConfigSpelEvaluatorFactory(ActionRunnerContextGlobal global) {
            this.global = global;
        }
        
        void reconfigureWithContext(ActionRunnerContextLocal ctx) {
            this.actionRunnerContext = ctx;
            // Re-register variables that need the full context reference
            getSpelEvaluator().configure(spelCtx -> {
                configureSpelContext(spelCtx, global.getConfig().getActionContextSpelEvaluatorConfigurers(), ctx);
                spelCtx.setVariable("action", new ActionRunnerContextSpelFunctions(ctx));
                ActionCiSpelFunctionsRegistry.registerRuntimeVariables(spelCtx, ctx);
            });
        }
        
        protected final void configureSpelContext(SimpleEvaluationContext spelContext) {
            var config = global.getConfig();
            configureSpelContext(spelContext, config.getActionConfigSpelEvaluatorConfigurers(), config);
            // actionRunnerContext may be null during initial creation; reconfigureWithContext handles that
            if (actionRunnerContext != null) {
                configureSpelContext(spelContext, config.getActionContextSpelEvaluatorConfigurers(), actionRunnerContext);
                spelContext.setVariable("action", new ActionRunnerContextSpelFunctions(actionRunnerContext));
                ActionCiSpelFunctionsRegistry.registerRuntimeVariables(spelContext, actionRunnerContext);
            } else {
                ActionCiSpelFunctionsRegistry.registerInfoVariables(spelContext);
            }
            spelContext.setVariable("fs", ActionFileSystemSpelFunctions.INSTANCE);
            spelContext.setVariable("fcli", FcliCommandsSpelFunctions.INSTANCE);
        }
    }
    
    @Override
    public void close() {
        getRequestHelpers().values().forEach(IActionRequestHelper::close);
    }
}
