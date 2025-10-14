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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fortify.cli.common.action.model.ActionStepWith;
import com.fortify.cli.common.action.model.ActionStepWithWriter;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.processor.writer.ActionStepRecordWriterFactory;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data
public class ActionStepWithWriterHandler implements IActionStepWithHandler {
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final String id;
    private final ActionStepWithWriter withWriter;
    
    public static final List<? extends IActionStepWithHandler> createHandlers(ActionStepProcessorWith actionStepProcessorWith, ActionRunnerContext ctx, ActionRunnerVars vars, ActionStepWith withStep) {
        var withWriters = withStep.getWriters();
        return withWriters==null || withWriters.isEmpty() 
                ? Collections.emptyList() 
                : withWriters.entrySet().stream()
                    .filter(actionStepProcessorWith::_if)
                    .map(e->new ActionStepWithWriterHandler(ctx, vars, e.getKey(), e.getValue()))
                    .toList();
    }

    @Override
    public final void doBefore() {
        ctx.getWriters().put(id, ActionStepRecordWriterFactory.createWriter(ctx, vars, withWriter));
        vars.set(String.format("%s.count", id), new IntNode(0));
    }
    
    @Override
    public final void doAfter() {
        var writer = ctx.getWriters().remove(id);
        if (writer!=null) { writer.close(); }
    }
    
    @Override
    public final boolean isAddShutdownHandler() {
        return true;
    }
}
