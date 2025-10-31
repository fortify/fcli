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
package com.fortify.cli.common.action.runner.processor;

import java.util.ArrayList;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepWith;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorWith extends AbstractActionStepProcessor {
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final ActionStepWith withStep;

    @Override
    public void process() {
        var handlers = new ArrayList<IActionStepWithHandler>();
        handlers.addAll(ActionStepWithCleanupHandler.createHandlers(this, ctx, vars, withStep));
        handlers.addAll(ActionStepWithSessionHandler.createHandlers(this, ctx, vars, withStep));
        handlers.addAll(ActionStepWithWriterHandler.createHandlers(this, ctx, vars, withStep));
        var shutdownThread = registerShutdownThread(handlers);
        try {
            handlers.forEach(IActionStepWithHandler::doBefore);
            processSteps(withStep.get_do());
        } finally {
            handlers.forEach(IActionStepWithHandler::doAfter);
            if ( shutdownThread!=null ) {
                Runtime.getRuntime().removeShutdownHook(shutdownThread);
            }
        }
    }

    private Thread registerShutdownThread(ArrayList<IActionStepWithHandler> handlers) {
        var shutdownHandlers = handlers.stream()
                .filter(IActionStepWithHandler::isAddShutdownHandler)
                .<Runnable>map(h->()->h.doAfter()).toList();
        var shutdownThread = shutdownHandlers.isEmpty() ? null : new Thread(()->{
            System.out.println("\n");
            shutdownHandlers.forEach(Runnable::run);
        });
        if ( shutdownThread!=null ) { Runtime.getRuntime().addShutdownHook(shutdownThread); }
        return shutdownThread;
    }
}
